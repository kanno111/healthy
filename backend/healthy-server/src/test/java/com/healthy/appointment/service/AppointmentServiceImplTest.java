package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.service.impl.AppointmentServiceImpl;
import com.healthy.appointment.vo.PatientAppointmentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.EMPTY;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.MISSING;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.SUCCESS;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AppointmentStockService appointmentStockService;

    @Mock
    private AppointmentStockRepairService appointmentStockRepairService;

    @Test
    void createDecreasesCapacityAndCreatesBookedAppointment() {
        AppointmentService service = createService();
        AppointmentCreateDTO request = request(10L);
        DoctorScheduleSlot slot = availableSlot(10L, 2);
        PatientAppointmentVO expected = new PatientAppointmentVO();
        expected.setId(20L);
        expected.setStatus("BOOKED");

        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot);
        when(appointmentStockService.preDeduct(10L)).thenReturn(SUCCESS);
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
        assertThat(saved.getRequestId()).isEqualTo("request-10");
        assertThat(saved.getStatus()).isEqualTo("BOOKED");
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createRejectsFullSlotBeforeDecreasingCapacity() {
        AppointmentService service = createService();
        DoctorScheduleSlot slot = availableSlot(10L, 0);
        when(appointmentStockService.preDeduct(10L)).thenReturn(SUCCESS);
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot);

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
        verify(appointmentMapper, never()).insert(any());
    }

    @Test
    void createRejectsWhenCapacityChangesBeforeUpdate() {
        AppointmentService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(availableSlot(10L, 1));
        when(appointmentStockService.preDeduct(10L)).thenReturn(SUCCESS);
        when(doctorScheduleSlotMapper.decreaseRemainingCapacity(10L)).thenReturn(0);

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(appointmentMapper, never()).insert(any());
        verify(appointmentStockRepairService).triggerAfterMysqlStockReject(10L);
    }

    @Test
    void createRejectsWhenRedisStockIsInsufficientWithoutChangingInventory() {
        AppointmentService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentStockService.preDeduct(10L)).thenReturn(EMPTY);

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).findById(10L);
        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
        verify(appointmentMapper, never()).insert(any());
        verify(appointmentStockRepairService).triggerIfEmpty(10L);
    }

    @Test
    void createReloadsMissingRedisStockWithSetNxAndRetriesLuaOnce() {
        AppointmentService service = createService();
        PatientAppointmentVO expected = new PatientAppointmentVO();
        expected.setId(20L);
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(availableSlot(10L, 2));
        when(appointmentStockService.preDeduct(10L)).thenReturn(MISSING, SUCCESS);
        when(doctorScheduleSlotMapper.decreaseRemainingCapacity(10L)).thenReturn(1);
        doAnswer(invocation -> {
            invocation.getArgument(0, Appointment.class).setId(20L);
            return 1;
        }).when(appointmentMapper).insert(any(Appointment.class));
        when(appointmentMapper.findByIdAndPatientId(20L, 5L)).thenReturn(expected);

        assertThat(service.create(1L, request(10L))).isSameAs(expected);

        verify(appointmentStockService).initializeIfAbsent(10L, 2);
        verify(appointmentStockService, times(2)).preDeduct(10L);
    }

    @Test
    void createRejectsMissingRedisStockWhenMysqlHasNoRemainingCapacity() {
        AppointmentService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentStockService.preDeduct(10L)).thenReturn(MISSING);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(availableSlot(10L, 0));

        assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(appointmentStockService, never()).initializeIfAbsent(10L, 0);
        verify(appointmentStockService, times(1)).preDeduct(10L);
    }

    @Test
    void createReturnsExistingAppointmentForRepeatedRequestId() {
        AppointmentService service = createService();
        PatientAppointmentVO expected = new PatientAppointmentVO();
        expected.setId(20L);
        expected.setStatus("BOOKED");
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentMapper.findByRequestIdAndPatientId("request-10", 5L)).thenReturn(expected);

        PatientAppointmentVO result = service.create(1L, request(10L));

        assertThat(result).isSameAs(expected);
        verify(appointmentStockService, never()).preDeduct(10L);
        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
        verify(appointmentMapper, never()).insert(any());
    }

    @Test
    void createReturnsExistingActiveAppointmentForAnotherRepeatedSubmission() {
        AppointmentService service = createService();
        PatientAppointmentVO expected = new PatientAppointmentVO();
        expected.setId(20L);
        expected.setStatus("BOOKED");
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentMapper.findActiveByPatientIdAndScheduleSlotId(5L, 10L)).thenReturn(expected);

        PatientAppointmentVO result = service.create(1L, request(10L));

        assertThat(result).isSameAs(expected);
        verify(appointmentStockService, never()).preDeduct(10L);
        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
    }

    @Test
    void createRestoresRedisStockWhenDatabaseTransactionRollsBack() {
        AppointmentService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(availableSlot(10L, 1));
        when(appointmentStockService.preDeduct(10L)).thenReturn(SUCCESS);
        when(doctorScheduleSlotMapper.decreaseRemainingCapacity(10L)).thenReturn(0);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.create(1L, request(10L))).isInstanceOf(BusinessException.class);
            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                    synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(appointmentStockService).restore(10L);
        verify(appointmentStockRepairService).triggerAfterMysqlStockReject(10L);
    }

    private AppointmentService createService() {
        return new AppointmentServiceImpl(patientMapper, doctorScheduleSlotMapper, appointmentMapper,
                appointmentStockService, appointmentStockRepairService);
    }

    private AppointmentCreateDTO request(Long slotId) {
        AppointmentCreateDTO request = new AppointmentCreateDTO();
        request.setScheduleSlotId(slotId);
        request.setRequestId("request-" + slotId);
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

    private Patient enabledPatient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setStatus(1);
        return patient;
    }
}
