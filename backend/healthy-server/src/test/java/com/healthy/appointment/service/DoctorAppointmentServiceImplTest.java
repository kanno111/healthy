package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.domain.booking.BookingViewAssembler;
import com.healthy.appointment.domain.provider.DoctorSummary;
import com.healthy.appointment.domain.provider.ProviderDirectory;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
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
    private ProviderDirectory providerDirectory;
    private AppointmentMapper appointmentMapper;
    private BookingViewAssembler bookingViewAssembler;
    private DoctorAppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        providerDirectory = mock(ProviderDirectory.class);
        appointmentMapper = mock(AppointmentMapper.class);
        bookingViewAssembler = mock(BookingViewAssembler.class);
        service = new DoctorAppointmentServiceImpl(providerDirectory, appointmentMapper, bookingViewAssembler);
    }

    @Test
    void pagesOnlyAppointmentsOwnedByDoctorResolvedFromCurrentUser() {
        DoctorSummary doctor = doctor(8L, 100L);
        DoctorAppointmentVO record = new DoctorAppointmentVO();
        record.setId(20L);
        Page<DoctorAppointmentVO> mapperPage = new Page<>(2, 10);
        mapperPage.setRecords(List.of(record));
        mapperPage.setTotal(11);
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenReturn(doctor);
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
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenReturn(doctor(8L, 100L));
        when(appointmentMapper.completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class))).thenReturn(1);

        service.complete(100L, 20L);

        verify(appointmentMapper).completeBookedByDoctor(
                eq(20L), eq(8L), any(LocalDate.class), any(LocalTime.class));
        verify(appointmentMapper, never()).findById(20L);
    }

    @Test
    void rejectsAppointmentOwnedByAnotherDoctorWhenConditionalUpdateAffectsNoRows() {
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenReturn(doctor(8L, 100L));
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
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenReturn(doctor(8L, 100L));
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
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenReturn(doctor(8L, 100L));
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
        when(providerDirectory.requireEnabledDoctorByUserId(100L)).thenThrow(new BusinessException(com.healthy.appointment.enumeration.ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> service.pageMine(100L, null, null, 1, 10))
                .isInstanceOf(BusinessException.class);

        verify(appointmentMapper, never()).pageForDoctor(any(), any(), any(), any());
    }

    private DoctorSummary doctor(Long doctorId, Long userId) {
        DoctorSummary doctor = new DoctorSummary();
        doctor.setId(doctorId);
        doctor.setUserId(userId);
        doctor.setStatus(1);
        return doctor;
    }
}
