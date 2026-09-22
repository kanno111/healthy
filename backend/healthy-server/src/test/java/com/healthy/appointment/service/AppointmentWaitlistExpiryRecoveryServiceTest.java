package com.healthy.appointment.service;

import com.healthy.appointment.config.AppointmentWaitlistProperties;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentWaitlistExpiryRecoveryServiceTest {
    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        MybatisPlusTestHelper.initializeTableInfo(AppointmentWaitlist.class);
    }

    @Test
    void continuesWithLaterCandidatesWhenOneIndependentTransactionFails() {
        AppointmentWaitlistMapper mapper = mock(AppointmentWaitlistMapper.class);
        AppointmentWaitlistService waitlistService = mock(AppointmentWaitlistService.class);
        AppointmentWaitlistProperties properties = new AppointmentWaitlistProperties();
        AppointmentWaitlist first = candidate(20L);
        AppointmentWaitlist second = candidate(21L);
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        when(waitlistService.expireOffered(20L)).thenThrow(new IllegalStateException("database unavailable"));
        when(waitlistService.expireOffered(21L)).thenReturn(true);

        AppointmentWaitlistExpiryRecoveryService recoveryService =
                new AppointmentWaitlistExpiryRecoveryService(mapper, waitlistService, properties);

        AppointmentWaitlistExpiryRecoveryService.RecoveryResult result = recoveryService.recoverDueOffers();

        assertThat(result).isEqualTo(new AppointmentWaitlistExpiryRecoveryService.RecoveryResult(2, 1, 0, 1));
        verify(waitlistService).expireOffered(20L);
        verify(waitlistService).expireOffered(21L);
    }

    private AppointmentWaitlist candidate(Long id) {
        AppointmentWaitlist waitlist = new AppointmentWaitlist();
        waitlist.setId(id);
        return waitlist;
    }
}
