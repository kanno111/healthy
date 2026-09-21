package com.healthy.appointment.scheduler;

import com.healthy.appointment.service.AppointmentWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically expires unconfirmed waitlist offers and advances the FIFO queue. */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentWaitlistExpiryScheduler {
    private final AppointmentWaitlistService appointmentWaitlistService;

    @Scheduled(fixedDelayString = "${appointment.waitlist.offer-expire-check-delay:5s}")
    public void expireDueOffers() {
        try {
            appointmentWaitlistService.expireDueOffers();
        } catch (RuntimeException exception) {
            log.error("Waitlist offer expiry task failed: operation=EXPIRE_DUE_OFFERS, reason={}",
                    exception.getMessage(), exception);
        }
    }
}
