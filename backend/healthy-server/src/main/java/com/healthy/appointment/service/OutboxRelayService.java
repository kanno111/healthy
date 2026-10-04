package com.healthy.appointment.service;

import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.mq.WaitlistTimeoutMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayService {
    private final OutboxMessageService outboxMessageService;
    private final WaitlistTimeoutMessagePublisher waitlistTimeoutMessagePublisher;

    public int dispatchBatch() {
        List<OutboxMessage> claimed = outboxMessageService.claimDispatchable();
        for (OutboxMessage message : claimed) {
            try {
                waitlistTimeoutMessagePublisher.publish(message);
            } catch (RuntimeException exception) {
                // Publisher normally records synchronous failures itself. This catch protects the
                // rest of the batch if a future publisher implementation leaks an exception.
                log.error("Unexpected outbox relay failure: outboxId={}, eventId={}",
                        message.getId(), message.getEventId(), exception);
                outboxMessageService.markFailed(message.getId(), message.getAttemptCount(),
                        exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }
        return claimed.size();
    }
}
