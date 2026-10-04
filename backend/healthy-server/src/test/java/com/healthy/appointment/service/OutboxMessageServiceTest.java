package com.healthy.appointment.service;

import com.healthy.appointment.config.OutboxProperties;
import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.enumeration.OutboxStatus;
import com.healthy.appointment.mapper.OutboxMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxMessageServiceTest {
    @Test
    void recordsWaitlistTimeoutAsPendingWithAbsoluteDeadline() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxMessageService service = service(mapper, new OutboxProperties());
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(1);
        when(mapper.insert(any(OutboxMessage.class))).thenReturn(1);

        service.recordWaitlistTimeout(20L, 10L, deadline);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(mapper).insert(captor.capture());
        OutboxMessage saved = captor.getValue();
        assertThat(saved.getEventId()).isNotBlank();
        assertThat(saved.getAggregateType()).isEqualTo("WAITLIST");
        assertThat(saved.getAggregateId()).isEqualTo(20L);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING.name());
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getScheduledAt()).isEqualTo(deadline);
        assertThat(saved.getPayload()).contains("\"waitlistId\":20", "\"scheduleSlotId\":10");
    }

    @Test
    void claimsDispatchableMessageAndIncrementsAttemptFence() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxMessageService service = service(mapper, new OutboxProperties());
        OutboxMessage candidate = new OutboxMessage();
        candidate.setId(1L);
        candidate.setAttemptCount(0);
        when(mapper.selectDispatchable(any(LocalDateTime.class), eq(50)))
                .thenReturn(List.of(candidate));
        when(mapper.markProcessing(eq(1L), any(LocalDateTime.class))).thenReturn(1);

        List<OutboxMessage> claimed = service.claimDispatchable();

        assertThat(claimed).singleElement().satisfies(message -> {
            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PROCESSING.name());
            assertThat(message.getAttemptCount()).isEqualTo(1);
            assertThat(message.getProcessingStartedAt()).isNotNull();
        });
        verify(mapper).recoverTimedOutProcessing(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(5));
    }

    @Test
    void failedAttemptIsScheduledForRetryBeforeLimit() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxMessageService service = service(mapper, new OutboxProperties());
        when(mapper.markFailed(eq(1L), eq(1), eq(OutboxStatus.RETRY.name()),
                any(LocalDateTime.class), eq("connection refused"))).thenReturn(1);

        service.markFailed(1L, 1, "connection refused");

        verify(mapper).markFailed(eq(1L), eq(1), eq(OutboxStatus.RETRY.name()),
                any(LocalDateTime.class), eq("connection refused"));
    }

    @Test
    void failedAttemptBecomesDeadAtConfiguredLimit() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxProperties properties = new OutboxProperties();
        properties.setMaxAttempts(3);
        OutboxMessageService service = service(mapper, properties);
        when(mapper.markFailed(eq(1L), eq(3), eq(OutboxStatus.DEAD.name()),
                any(LocalDateTime.class), eq("unroutable"))).thenReturn(1);

        service.markFailed(1L, 3, "unroutable");

        verify(mapper).markFailed(eq(1L), eq(3), eq(OutboxStatus.DEAD.name()),
                any(LocalDateTime.class), eq("unroutable"));
    }

    private OutboxMessageService service(OutboxMessageMapper mapper, OutboxProperties properties) {
        return new OutboxMessageService(mapper, new ObjectMapper(), properties);
    }
}
