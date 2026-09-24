package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.impl.DoctorAppointmentServiceImpl;
import com.healthy.appointment.vo.DoctorAppointmentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorAppointmentServiceImplTest {
    private DoctorMapper doctorMapper;
    private AppointmentMapper appointmentMapper;
    private DoctorAppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        doctorMapper = mock(DoctorMapper.class);
        appointmentMapper = mock(AppointmentMapper.class);
        service = new DoctorAppointmentServiceImpl(doctorMapper, appointmentMapper);
    }

    @Test
    void pagesOnlyAppointmentsOwnedByDoctorResolvedFromCurrentUser() {
        Doctor doctor = doctor(8L, 100L);
        DoctorAppointmentVO record = new DoctorAppointmentVO();
        record.setId(20L);
        Page<DoctorAppointmentVO> mapperPage = new Page<>(2, 10);
        mapperPage.setRecords(List.of(record));
        mapperPage.setTotal(11);
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(doctor);
        when(appointmentMapper.pageForDoctor(any(), eq(8L), eq(LocalDate.of(2026, 9, 22)), eq("BOOKED")))
                .thenReturn(mapperPage);

        PageResult<DoctorAppointmentVO> result = service.pageMine(
                100L, LocalDate.of(2026, 9, 22), "booked", 2, 10);

        assertThat(result.records()).containsExactly(record);
        assertThat(result.total()).isEqualTo(11);
        ArgumentCaptor<IPage<DoctorAppointmentVO>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(appointmentMapper).pageForDoctor(
                pageCaptor.capture(), eq(8L), eq(LocalDate.of(2026, 9, 22)), eq("BOOKED"));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
    }

    @Test
    void completesOnlyBookedAppointmentOwnedByCurrentDoctor() {
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(doctor(8L, 100L));
        when(appointmentMapper.completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class))).thenReturn(1);

        service.complete(100L, 20L);

        verify(appointmentMapper).completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class));
        verify(appointmentMapper, never()).findById(20L);
    }

    @Test
    void rejectsAppointmentOwnedByAnotherDoctorWhenConditionalUpdateAffectsNoRows() {
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(doctor(8L, 100L));
        when(appointmentMapper.completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class))).thenReturn(0);
        Appointment appointment = new Appointment();
        appointment.setId(20L);
        appointment.setDoctorId(9L);
        appointment.setStatus("BOOKED");
        when(appointmentMapper.findById(20L)).thenReturn(appointment);

        assertThatThrownBy(() -> service.complete(100L, 20L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsRepeatedCompletionWhenConditionalUpdateAffectsNoRows() {
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(doctor(8L, 100L));
        when(appointmentMapper.completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class))).thenReturn(0);
        Appointment appointment = new Appointment();
        appointment.setId(20L);
        appointment.setDoctorId(8L);
        appointment.setStatus("COMPLETED");
        when(appointmentMapper.findById(20L)).thenReturn(appointment);

        assertThatThrownBy(() -> service.complete(100L, 20L))
                .isInstanceOf(BusinessException.class);

        verify(appointmentMapper).completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void rejectsCompletingFutureAppointment() {
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(doctor(8L, 100L));
        Appointment appointment = new Appointment();
        appointment.setId(20L);
        appointment.setDoctorId(8L);
        appointment.setStatus("BOOKED");
        appointment.setScheduleDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(8, 0));
        when(appointmentMapper.findById(20L)).thenReturn(appointment);

        assertThatThrownBy(() -> service.complete(100L, 20L))
                .isInstanceOf(BusinessException.class);

        verify(appointmentMapper).completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void rejectsAccountWithoutBoundDoctorProfile() {
        when(doctorMapper.findEnabledByUserId(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.pageMine(100L, null, null, 1, 10))
                .isInstanceOf(BusinessException.class);

        verify(appointmentMapper, never()).pageForDoctor(any(), any(), any(), any());
    }

    private Doctor doctor(Long doctorId, Long userId) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setUserId(userId);
        doctor.setStatus(1);
        return doctor;
    }
}
