package com.healthy.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "appointment.outbox")
public class OutboxProperties {
    private boolean relayEnabled = true;
    private Duration relayDelay = Duration.ofSeconds(1);
    private int batchSize = 50;
    private int maxAttempts = 5;
    private Duration retryInitialDelay = Duration.ofSeconds(5);
    private double retryMultiplier = 3.0;
    private Duration retryMaxDelay = Duration.ofMinutes(5);
    private Duration processingTimeout = Duration.ofMinutes(1);

    public boolean isRelayEnabled() {
        return relayEnabled;
    }

    public void setRelayEnabled(boolean relayEnabled) {
        this.relayEnabled = relayEnabled;
    }

    public Duration getRelayDelay() {
        return relayDelay;
    }

    public void setRelayDelay(Duration relayDelay) {
        this.relayDelay = relayDelay;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryInitialDelay() {
        return retryInitialDelay;
    }

    public void setRetryInitialDelay(Duration retryInitialDelay) {
        this.retryInitialDelay = retryInitialDelay;
    }

    public double getRetryMultiplier() {
        return retryMultiplier;
    }

    public void setRetryMultiplier(double retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
    }

    public Duration getRetryMaxDelay() {
        return retryMaxDelay;
    }

    public void setRetryMaxDelay(Duration retryMaxDelay) {
        this.retryMaxDelay = retryMaxDelay;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }
}
