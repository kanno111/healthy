package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.service.impl.AppointmentServiceImpl;
import com.healthy.appointment.vo.PatientAppointmentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Mock
    private AppointmentMapper appointmentMapper;

    @Test
    void createDecreasesCapacityAndCreatesConfirmedAppointment() {
        AppointmentService service = createService();
        AppointmentCreateDTO request = request(10L);
        DoctorScheduleSlot slot = availableSlot(10L, 2);
        PatientAppointmentVO expected = new PatientAppointmentVO();
        expected.setId(20L);
        expected.setStatus("CONFIRMED");

        when(patientMapper.findEnabledIdByUserId(1L)).thenReturn(5L);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot);
        when(doctorScheduleSlotMapper.decreaseRemainingCapacity(10L)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.getArgument(0, Appointment.class).setId(20L);
            return 1;
        }).when(appointmentMapper).insert(any(Appointment.class));
        when(appointmentMapper.findByIdAndPatientId(20L, 5L)).thenReturn(expected);

        PatientAppointmentVO result = service.create(1L, request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentMapper).insert(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getAppointmentNo()).hasSize(32);
        assertThat(saved.getPatientId()).isEqualTo(5L);
        assertThat(saved.getDoctorId()).isEqualTo(3L);
        assertThat(saved.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createRejectsFullSlotBeforeDecreasingCapacity() {
        AppointmentService service = createService();
        DoctorScheduleSlot slot = availableSlot(10L, 0);
        when(patientMapper.findEnabledIdByUserId(1L)).thenReturn(5L);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot);

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
        verify(appointmentMapper, never()).insert(any());
    }

    @Test
    void createRejectsWhenCapacityChangesBeforeUpdate() {
        AppointmentService service = createService();
        when(patientMapper.findEnabledIdByUserId(1L)).thenReturn(5L);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(availableSlot(10L, 1));
        when(doctorScheduleSlotMapper.decreaseRemainingCapacity(10L)).thenReturn(0);

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(appointmentMapper, never()).insert(any());
    }

    private AppointmentService createService() {
        return new AppointmentServiceImpl(patientMapper, doctorScheduleSlotMapper, appointmentMapper);
    }

    private AppointmentCreateDTO request(Long slotId) {
        AppointmentCreateDTO request = new AppointmentCreateDTO();
        request.setScheduleSlotId(slotId);
        return request;
    }

    private DoctorScheduleSlot availableSlot(Long id, int remainingCapacity) {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setId(id);
        slot.setDoctorId(3L);
        slot.setScheduleDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(12, 0));
        slot.setStatus("OPEN");
        slot.setRemainingCapacity(remainingCapacity);
        return slot;
    }
}
