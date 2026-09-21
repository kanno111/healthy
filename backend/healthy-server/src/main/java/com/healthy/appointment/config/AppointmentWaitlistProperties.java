package com.healthy.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Timing settings for the database-backed waitlist flow. */
@ConfigurationProperties(prefix = "appointment.waitlist")
public class AppointmentWaitlistProperties {
    private Duration offerDuration = Duration.ofSeconds(10);

    public Duration getOfferDuration() {
        return offerDuration;
    }

    public void setOfferDuration(Duration offerDuration) {
        this.offerDuration = offerDuration;
    }
}
