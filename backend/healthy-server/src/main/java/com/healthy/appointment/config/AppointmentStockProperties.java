package com.healthy.appointment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** 号源缓存延迟修复的轻量级配置。 */
@ConfigurationProperties(prefix = "appointment.stock")
public class AppointmentStockProperties {
    private Duration repairDelay = Duration.ofSeconds(3);
    private Duration repairCheckTtl = Duration.ofSeconds(8);

    public Duration getRepairDelay() {
        return repairDelay;
    }

    public void setRepairDelay(Duration repairDelay) {
        this.repairDelay = repairDelay;
    }

    public Duration getRepairCheckTtl() {
        return repairCheckTtl;
    }

    public void setRepairCheckTtl(Duration repairCheckTtl) {
        this.repairCheckTtl = repairCheckTtl;
    }
}
