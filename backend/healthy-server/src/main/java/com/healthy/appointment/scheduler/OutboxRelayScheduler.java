package com.healthy.appointment.scheduler;

import com.healthy.appointment.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "appointment.outbox", name = "relay-enabled",
        havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {
    private final OutboxRelayService outboxRelayService;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(fixedDelayString = "${appointment.outbox.relay-delay:1s}")
    public void relay() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipped overlapping outbox relay execution");
            return;
        }
        try {
            int dispatched = outboxRelayService.dispatchBatch();
            if (dispatched > 0) {
                log.info("Outbox relay batch submitted: count={}", dispatched);
            }
        } catch (RuntimeException exception) {
            log.error("Outbox relay task failed: reason={}", exception.getMessage(), exception);
        } finally {
            running.set(false);
        }
    }
}
