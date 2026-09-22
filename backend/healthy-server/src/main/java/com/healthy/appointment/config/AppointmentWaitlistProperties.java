package com.healthy.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Timing settings for the waitlist offer flow. */
@ConfigurationProperties(prefix = "appointment.waitlist")
public class AppointmentWaitlistProperties {
    private Duration offerDuration = Duration.ofSeconds(10);
    private int recoveryBatchSize = 100;

    public Duration getOfferDuration() {
        return offerDuration;
    }

    public void setOfferDuration(Duration offerDuration) {
        this.offerDuration = offerDuration;
    }

    public int getRecoveryBatchSize() {
        return recoveryBatchSize;
    }

    public void setRecoveryBatchSize(int recoveryBatchSize) {
        this.recoveryBatchSize = recoveryBatchSize;
    }
}
