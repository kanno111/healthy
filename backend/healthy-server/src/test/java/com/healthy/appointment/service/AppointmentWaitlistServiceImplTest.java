package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentWaitlistCreateDTO;
import com.healthy.appointment.config.AppointmentWaitlistProperties;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.service.impl.AppointmentWaitlistServiceImpl;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_OFFERED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_WAITING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentWaitlistServiceImplTest {
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentWaitlistMapper appointmentWaitlistMapper;
    @Mock
    private AppointmentStockService appointmentStockService;

    @Test
    void joinCreatesWaitingRecordForFullOpenFutureSlot() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist saved = waitlist(20L);

        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(null, saved);
        when(appointmentWaitlistMapper.findByIdAndPatientId(20L, 5L)).thenReturn(waitlistVO(20L));
        when(appointmentWaitlistMapper.insert(any(AppointmentWaitlist.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, AppointmentWaitlist.class).setId(20L);
            return 1;
        });

        PatientAppointmentWaitlistVO result = service.join(1L, request(10L));
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getStatus()).isEqualTo(WAITLIST_STATUS_WAITING);

        ArgumentCaptor<AppointmentWaitlist> captor = ArgumentCaptor.forClass(AppointmentWaitlist.class);
        verify(appointmentWaitlistMapper).insert(captor.capture());
        assertThat(captor.getValue().getPatientId()).isEqualTo(5L);
        assertThat(captor.getValue().getScheduleSlotId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(WAITLIST_STATUS_WAITING);
        assertThat(captor.getValue().getOfferExpireTime()).isNull();
    }

    @Test
    void joinReturnsExistingActiveWaitlistWithoutInsertingAgain() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist existing = waitlist(20L);

        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(existing);
        when(appointmentWaitlistMapper.findByIdAndPatientId(20L, 5L)).thenReturn(waitlistVO(20L));

        assertThat(service.join(1L, request(10L)).getId()).isEqualTo(existing.getId());

        verify(appointmentWaitlistMapper, never()).insert(any(AppointmentWaitlist.class));
    }

    @Test
    void joinReturnsConcurrentActiveWaitlistAfterUniqueKeyConflict() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist existing = waitlist(20L);

        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(null, existing);
        when(appointmentWaitlistMapper.findByIdAndPatientId(20L, 5L)).thenReturn(waitlistVO(20L));
        when(appointmentWaitlistMapper.insert(any(AppointmentWaitlist.class))).thenThrow(new DuplicateKeyException("active waitlist exists"));

        assertThat(service.join(1L, request(10L)).getId()).isEqualTo(existing.getId());
    }

    @Test
    void joinRejectsSlotThatStillHasPublicCapacity() {
        AppointmentWaitlistService service = createService();
        DoctorScheduleSlot slot = fullOpenFutureSlot(10L);
        slot.setRemainingCapacity(1);
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot);

        assertThatThrownBy(() -> service.join(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(appointmentWaitlistMapper, never()).insert(any(AppointmentWaitlist.class));
    }

    @Test
    void joinRejectsPatientWithBookedAppointmentForTheSameSlot() {
        AppointmentWaitlistService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentMapper.findActiveByPatientIdAndScheduleSlotId(5L, 10L))
                .thenReturn(new com.healthy.appointment.vo.PatientAppointmentVO());

        assertThatThrownBy(() -> service.join(1L, request(10L))).isInstanceOf(BusinessException.class);

        verify(appointmentWaitlistMapper, never()).insert(any(AppointmentWaitlist.class));
    }

    @Test
    void cancelChangesOnlyWaitingWaitlistToCancelled() {
        AppointmentWaitlistService service = createService();
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(waitlist(20L));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1);

        service.cancel(1L, 20L);

        verify(appointmentWaitlistMapper).update(org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void cancelRejectsOfferedWaitlist() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist offered = waitlist(20L);
        offered.setStatus(WAITLIST_STATUS_OFFERED);
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(offered);

        assertThatThrownBy(() -> service.cancel(1L, 20L)).isInstanceOf(BusinessException.class);

        verify(appointmentWaitlistMapper, never()).update(org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void offerFirstWaitingMovesOnlyTheLockedFirstCandidateToOffered() {
        AppointmentWaitlistService service = createService();
        when(appointmentWaitlistMapper.selectFirstWaitingForUpdate(10L)).thenReturn(waitlist(20L));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1);

        assertThat(service.offerFirstWaiting(10L)).isTrue();

        verify(appointmentWaitlistMapper).selectFirstWaitingForUpdate(10L);
        verify(appointmentWaitlistMapper).update(org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void confirmCreatesAppointmentWithoutDeductingPublicStock() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist offered = waitlist(20L);
        offered.setStatus(WAITLIST_STATUS_OFFERED);
        offered.setOfferExpireTime(LocalDateTime.now().plusMinutes(5));
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(offered);
        when(appointmentMapper.findActiveByPatientIdAndScheduleSlotId(5L, 10L)).thenReturn(null);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1);
        when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

        service.confirm(1L, 20L);

        verify(appointmentWaitlistMapper).update(org.mockito.ArgumentMatchers.isNull(), any());
        verify(appointmentMapper).insert(any(Appointment.class));
        verify(appointmentStockService, never()).preDeduct(10L);
        verify(doctorScheduleSlotMapper, never()).decreaseRemainingCapacity(10L);
    }

    @Test
    void confirmDoesNotCreateAppointmentWhenItLosesTheOfferStateRace() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist offered = waitlist(20L);
        offered.setStatus(WAITLIST_STATUS_OFFERED);
        offered.setOfferExpireTime(LocalDateTime.now().plusMinutes(5));
        when(patientMapper.selectOne(any())).thenReturn(enabledPatient(5L));
        when(appointmentWaitlistMapper.selectOne(any())).thenReturn(offered);
        when(appointmentMapper.findActiveByPatientIdAndScheduleSlotId(5L, 10L)).thenReturn(null);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(fullOpenFutureSlot(10L));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.confirm(1L, 20L)).isInstanceOf(BusinessException.class);

        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    @Test
    void expireOffersAdvancesToNextWaitingPatientWithoutRestoringPublicStock() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist expiredOffer = waitlist(20L);
        expiredOffer.setStatus(WAITLIST_STATUS_OFFERED);
        expiredOffer.setOfferExpireTime(LocalDateTime.now().minusMinutes(1));
        when(appointmentWaitlistMapper.selectList(any())).thenReturn(List.of(expiredOffer));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1, 1);
        when(appointmentWaitlistMapper.selectFirstWaitingForUpdate(10L)).thenReturn(waitlist(21L));

        service.expireDueOffers();

        verify(appointmentWaitlistMapper).selectFirstWaitingForUpdate(10L);
        verify(doctorScheduleSlotMapper, never()).increaseRemainingCapacity(10L);
        verify(appointmentStockService, never()).restore(10L);
    }

    @Test
    void expireOffersRestoresPublicStockOnceWhenNoWaitingPatientExists() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist expiredOffer = waitlist(20L);
        expiredOffer.setStatus(WAITLIST_STATUS_OFFERED);
        expiredOffer.setOfferExpireTime(LocalDateTime.now().minusMinutes(1));
        when(appointmentWaitlistMapper.selectList(any())).thenReturn(List.of(expiredOffer));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(1);
        when(appointmentWaitlistMapper.selectFirstWaitingForUpdate(10L)).thenReturn(null);
        when(doctorScheduleSlotMapper.increaseRemainingCapacity(10L)).thenReturn(1);

        service.expireDueOffers();

        verify(doctorScheduleSlotMapper).increaseRemainingCapacity(10L);
        verify(appointmentStockService).restore(10L);
    }

    @Test
    void expireOfferDoesNotReleaseStockWhenAnotherWorkerAlreadyChangedItsState() {
        AppointmentWaitlistService service = createService();
        AppointmentWaitlist expiredOffer = waitlist(20L);
        expiredOffer.setStatus(WAITLIST_STATUS_OFFERED);
        expiredOffer.setOfferExpireTime(LocalDateTime.now().minusMinutes(1));
        when(appointmentWaitlistMapper.selectList(any())).thenReturn(List.of(expiredOffer));
        when(appointmentWaitlistMapper.update(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(0);

        service.expireDueOffers();

        verify(appointmentWaitlistMapper, never()).selectFirstWaitingForUpdate(10L);
        verify(doctorScheduleSlotMapper, never()).increaseRemainingCapacity(10L);
        verify(appointmentStockService, never()).restore(10L);
    }

    private AppointmentWaitlistService createService() {
        return new AppointmentWaitlistServiceImpl(patientMapper, doctorScheduleSlotMapper,
                appointmentMapper, appointmentWaitlistMapper, appointmentStockService,
                new AppointmentWaitlistProperties());
    }

    private AppointmentWaitlistCreateDTO request(Long slotId) {
        AppointmentWaitlistCreateDTO request = new AppointmentWaitlistCreateDTO();
        request.setScheduleSlotId(slotId);
        return request;
    }

    private DoctorScheduleSlot fullOpenFutureSlot(Long id) {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setId(id);
        slot.setScheduleDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(12, 0));
        slot.setStatus("OPEN");
        slot.setRemainingCapacity(0);
        return slot;
    }

    private Patient enabledPatient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setStatus(1);
        return patient;
    }

    private AppointmentWaitlist waitlist(Long id) {
        AppointmentWaitlist waitlist = new AppointmentWaitlist();
        waitlist.setId(id);
        waitlist.setPatientId(5L);
        waitlist.setScheduleSlotId(10L);
        waitlist.setStatus(WAITLIST_STATUS_WAITING);
        return waitlist;
    }

    private PatientAppointmentWaitlistVO waitlistVO(Long id) {
        PatientAppointmentWaitlistVO vo = new PatientAppointmentWaitlistVO();
        vo.setId(id);
        vo.setStatus(WAITLIST_STATUS_WAITING);
        return vo;
    }
}
