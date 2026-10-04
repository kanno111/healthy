package com.healthy.appointment.service;

import com.healthy.appointment.config.OutboxProperties;
import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.enumeration.OutboxEventType;
import com.healthy.appointment.enumeration.OutboxStatus;
import com.healthy.appointment.mapper.OutboxMessageMapper;
import com.healthy.appointment.mq.WaitlistTimeoutMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxMessageService {
    private static final int MAX_BATCH_SIZE = 1000;
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final String WAITLIST_AGGREGATE = "WAITLIST";

    private final OutboxMessageMapper outboxMessageMapper;
    private final ObjectMapper objectMapper;
    private final OutboxProperties outboxProperties;

    /** Must join the business transaction that changes WAITING to OFFERED. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWaitlistTimeout(Long waitlistId, Long scheduleSlotId,
                                      LocalDateTime offerExpireTime) {
        OutboxMessage message = new OutboxMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setAggregateType(WAITLIST_AGGREGATE);
        message.setAggregateId(waitlistId);
        message.setEventType(OutboxEventType.WAITLIST_OFFER_TIMEOUT.name());
        message.setPayload(objectMapper.writeValueAsString(
                new WaitlistTimeoutMessage(waitlistId, scheduleSlotId)));
        message.setExchangeName(WaitlistRabbitMqConfig.DELAY_EXCHANGE);
        message.setRoutingKey(WaitlistRabbitMqConfig.DELAY_ROUTING_KEY);
        message.setScheduledAt(offerExpireTime);
        message.setStatus(OutboxStatus.PENDING.name());
        message.setAttemptCount(0);
        message.setNextRetryAt(LocalDateTime.now());
        if (outboxMessageMapper.insert(message) != 1) {
            throw new IllegalStateException("Unable to persist waitlist timeout outbox message");
        }
    }

    /**
     * Claims a batch for the single application instance. Attempt count is also the callback
     * fencing token, so a late confirm from an older attempt cannot overwrite a newer result.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OutboxMessage> claimDispatchable() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processingDeadline = now.minus(normalizedProcessingTimeout());
        int maxAttempts = Math.max(1, outboxProperties.getMaxAttempts());
        int recovered = outboxMessageMapper.recoverTimedOutProcessing(
                processingDeadline, now, maxAttempts);
        if (recovered > 0) {
            log.warn("Recovered timed-out outbox messages: count={}, processingDeadline={}",
                    recovered, processingDeadline);
        }

        int batchSize = Math.max(1, Math.min(outboxProperties.getBatchSize(), MAX_BATCH_SIZE));
        List<OutboxMessage> candidates = outboxMessageMapper.selectDispatchable(now, batchSize);
        List<OutboxMessage> claimed = new ArrayList<>(candidates.size());
        for (OutboxMessage candidate : candidates) {
            if (outboxMessageMapper.markProcessing(candidate.getId(), now) == 1) {
                candidate.setStatus(OutboxStatus.PROCESSING.name());
                candidate.setAttemptCount(candidate.getAttemptCount() + 1);
                candidate.setProcessingStartedAt(now);
                claimed.add(candidate);
            }
        }
        return claimed;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSent(Long outboxId, int attemptCount) {
        int updated = outboxMessageMapper.markSent(outboxId, attemptCount, LocalDateTime.now());
        if (updated == 0) {
            log.warn("Ignored stale outbox publisher ACK: outboxId={}, attemptCount={}",
                    outboxId, attemptCount);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long outboxId, int attemptCount, String failure) {
        int maxAttempts = Math.max(1, outboxProperties.getMaxAttempts());
        boolean exhausted = attemptCount >= maxAttempts;
        String status = exhausted ? OutboxStatus.DEAD.name() : OutboxStatus.RETRY.name();
        LocalDateTime nextRetryAt = exhausted
                ? LocalDateTime.now()
                : LocalDateTime.now().plus(retryDelay(attemptCount));
        int updated = outboxMessageMapper.markFailed(outboxId, attemptCount, status,
                nextRetryAt, truncate(failure));
        if (updated == 0) {
            log.warn("Ignored stale outbox publisher failure: outboxId={}, attemptCount={}, failure={}",
                    outboxId, attemptCount, truncate(failure));
        } else if (exhausted) {
            log.error("Outbox message exhausted all publish attempts: outboxId={}, attemptCount={}, status=DEAD, failure={}",
                    outboxId, attemptCount, truncate(failure));
        } else {
            log.warn("Outbox message scheduled for retry: outboxId={}, attemptCount={}, nextRetryAt={}, failure={}",
                    outboxId, attemptCount, nextRetryAt, truncate(failure));
        }
    }

    private Duration retryDelay(int attemptCount) {
        long initialMillis = Math.max(1, outboxProperties.getRetryInitialDelay().toMillis());
        long maximumMillis = Math.max(initialMillis, outboxProperties.getRetryMaxDelay().toMillis());
        double multiplier = Math.max(1.0, outboxProperties.getRetryMultiplier());
        double calculated = initialMillis * Math.pow(multiplier, Math.max(0, attemptCount - 1));
        return Duration.ofMillis((long) Math.min(calculated, maximumMillis));
    }

    private Duration normalizedProcessingTimeout() {
        Duration configured = outboxProperties.getProcessingTimeout();
        if (configured == null || configured.isZero() || configured.isNegative()) {
            return Duration.ofMinutes(1);
        }
        return configured;
    }

    private String truncate(String failure) {
        String value = failure == null || failure.isBlank() ? "Unknown publisher failure" : failure;
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
