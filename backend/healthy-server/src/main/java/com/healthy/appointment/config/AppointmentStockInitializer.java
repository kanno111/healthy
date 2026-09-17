package com.healthy.appointment.config;

import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.service.AppointmentStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 为部署前已存在的开放班次补齐缺失的 Redis 库存键，不覆盖已有实时库存。 */
@Component
@RequiredArgsConstructor
public class AppointmentStockInitializer implements ApplicationRunner {
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    private final AppointmentStockService appointmentStockService;

    @Override
    public void run(ApplicationArguments args) {
        doctorScheduleSlotMapper.listOpenFutureSlots().forEach(slot ->
                appointmentStockService.initializeIfAbsent(slot.getId(), slot.getRemainingCapacity()));
    }
}
