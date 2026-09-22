package com.healthy.appointment.scheduler;

import com.healthy.appointment.service.AppointmentWaitlistExpiryRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Low-frequency recovery for expired offers whose RabbitMQ timeout message was lost or failed. */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentWaitlistExpiryScheduler {
    private final AppointmentWaitlistExpiryRecoveryService recoveryService;

    @Scheduled(fixedDelayString = "${appointment.waitlist.offer-expire-check-delay:2m}")
    public void expireDueOffers() {
        try {
            AppointmentWaitlistExpiryRecoveryService.RecoveryResult result = recoveryService.recoverDueOffers();
            if (result.scanned() > 0) {
                log.info("Waitlist offer expiry recovery completed: scanned={}, expired={}, skipped={}, failed={}",
                        result.scanned(), result.expired(), result.skipped(), result.failed());
            }
        } catch (RuntimeException exception) {
            log.error("Waitlist offer expiry task failed: operation=EXPIRE_DUE_OFFERS, reason={}",
                    exception.getMessage(), exception);
        }
    }
}
