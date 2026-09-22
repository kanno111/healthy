package com.healthy.appointment.service;

import com.healthy.appointment.config.AppointmentStockProperties;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_OPEN;

/**
 * Redis 与 MySQL 号源库存不一致时的低频延迟校验。
 * 不直接覆盖 Redis 库存；确认不一致后仅删除当前值未变化的缓存，交给下一次请求懒加载。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentStockRepairService {
    private final AppointmentStockService appointmentStockService;
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Qualifier("appointmentStockRepairTaskScheduler")
    private final TaskScheduler taskScheduler;
    private final AppointmentStockProperties properties;

    /** 当前请求不等待修复；同一 slot 在 check TTL 内最多安排一个任务。 */
    public void triggerIfEmpty(Long slotId) {
        triggerRepair(slotId);
    }

    /**
     * Redis 预扣成功但 MySQL 条件更新失败时触发延迟校验。
     * 当前请求的补偿会先执行；该任务仅在延迟后检查是否仍然存在偏大库存。
     */
    public void triggerAfterMysqlStockReject(Long slotId) {
        triggerRepair(slotId);
    }

    private void triggerRepair(Long slotId) {
        try {
            if (!appointmentStockService.tryAcquireRepairCheck(slotId, properties.getRepairCheckTtl())) {
                return;
            }
            log.warn("Redis stock repair triggered: slotId={}, repairDelay={}, repairResult=SCHEDULED",
                    slotId, properties.getRepairDelay());
            taskScheduler.schedule(() -> repair(slotId), Instant.now().plus(properties.getRepairDelay()));
        } catch (RuntimeException exception) {
            // 自愈只是降级保护，调度或 Redis 标记异常不能改变本次“无号”的业务结果。
            log.error("Unable to schedule Redis stock repair: operation=SCHEDULE_REPAIR, slotId={}, reason={}",
                    slotId, exception.getMessage(), exception);
        }
    }

    // 包可见，便于不等待真实时间地进行单元测试。
    void repair(Long slotId) {
        try {
            Integer redisStock = appointmentStockService.getStock(slotId);
            if (redisStock == null) {
                return;
            }
            DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(slotId);
            Integer mysqlStock = slot == null ? null : slot.getRemainingCapacity();
            if (slot != null && SCHEDULE_STATUS_OPEN.equals(slot.getStatus())
                    && mysqlStock != null
                    && redisStock.equals(mysqlStock)) {
                return;
            }
            Integer delta = mysqlStock == null ? null : mysqlStock - redisStock;
            log.warn("Redis/MySQL stock mismatch detected: slotId={}, redisStock={}, mysqlStock={}, delta={}, repairResult=DELETE_PENDING",
                    slotId, redisStock, mysqlStock, delta);
            // Lua 内再次确认 Redis 值仍为本次观测值，避免并发挂号/退号后误删新库存。
            if (appointmentStockService.removeIfValue(slotId, redisStock)) {
                log.warn("Abnormal Redis stock key deleted; waiting for lazy rebuild: slotId={}, redisStock={}, mysqlStock={}, delta={}, repairResult=DELETED",
                        slotId, redisStock, mysqlStock, delta);
            } else {
                log.warn("Redis stock repair skipped because value changed concurrently: slotId={}, redisStock={}, mysqlStock={}, delta={}, repairResult=SKIPPED",
                        slotId, redisStock, mysqlStock, delta);
            }
        } catch (RuntimeException exception) {
            log.error("Redis stock repair failed: operation=REPAIR, slotId={}, reason={}",
                    slotId, exception.getMessage(), exception);
        }
    }
}
