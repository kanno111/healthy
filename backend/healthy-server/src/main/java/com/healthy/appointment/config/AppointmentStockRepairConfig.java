package com.healthy.appointment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 独立的小线程池，只用于延迟校验库存，不占用 Web 请求线程。 */
@Configuration
@EnableConfigurationProperties(AppointmentStockProperties.class)
public class AppointmentStockRepairConfig {
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler appointmentStockRepairTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("appointment-stock-repair-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }
}
