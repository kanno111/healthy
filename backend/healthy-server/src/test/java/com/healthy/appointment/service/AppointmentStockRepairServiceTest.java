package com.healthy.appointment.service;

import com.healthy.appointment.config.AppointmentStockProperties;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentStockRepairServiceTest {
    @Mock
    private AppointmentStockService appointmentStockService;
    @Mock
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Mock
    private TaskScheduler taskScheduler;

    @Test
    void equalRedisAndMysqlStockDoesNotDeleteStockKey() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.getStock(10L)).thenReturn(0);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(0));

        service.repair(10L);

        verify(appointmentStockService, never()).removeIfValue(anyLong(), anyInt());
    }

    @Test
    void emptyRedisAndPositiveMysqlDeletesOnlyWithObservedValueCheck() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.getStock(10L)).thenReturn(0);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(3));
        when(appointmentStockService.removeIfValue(10L, 0)).thenReturn(true);

        service.repair(10L);

        verify(appointmentStockService).removeIfValue(10L, 0);
    }

    @Test
    void redisLargerThanEmptyMysqlDeletesOnlyWithObservedValueCheck() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.getStock(10L)).thenReturn(3);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(0));
        when(appointmentStockService.removeIfValue(10L, 3)).thenReturn(true);

        service.repair(10L);

        verify(appointmentStockService).removeIfValue(10L, 3);
    }

    @Test
    void stockChangeDuringRepairPreventsDeletion() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.getStock(10L)).thenReturn(0);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(3));
        // Redis 的值在 compare-and-delete 前被另一请求更新，Lua 拒绝删除。
        when(appointmentStockService.removeIfValue(10L, 0)).thenReturn(false);

        service.repair(10L);

        verify(appointmentStockService).removeIfValue(10L, 0);
    }

    @Test
    void onlyOneRepairTaskIsScheduledForRepeatedEmptyRequests() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.tryAcquireRepairCheck(eq(10L), any(Duration.class)))
                .thenReturn(true, false, false);

        service.triggerIfEmpty(10L);
        service.triggerIfEmpty(10L);
        service.triggerIfEmpty(10L);

        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void scheduledRepairRunsAfterTheRequestReturns() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.tryAcquireRepairCheck(eq(10L), any(Duration.class))).thenReturn(true);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        service.triggerIfEmpty(10L);

        verify(taskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
        verify(doctorScheduleSlotMapper, never()).findById(10L);
        when(appointmentStockService.getStock(10L)).thenReturn(0);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(3));
        taskCaptor.getValue().run();
        verify(appointmentStockService).removeIfValue(10L, 0);
    }

    @Test
    void mysqlRejectSchedulesDelayedRepairForRedisLargerThanMysql() {
        AppointmentStockRepairService service = service();
        when(appointmentStockService.tryAcquireRepairCheck(eq(10L), any(Duration.class))).thenReturn(true);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        service.triggerAfterMysqlStockReject(10L);

        verify(taskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
        when(appointmentStockService.getStock(10L)).thenReturn(3);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(openSlot(0));
        taskCaptor.getValue().run();

        verify(appointmentStockService).removeIfValue(10L, 3);
    }

    private AppointmentStockRepairService service() {
        AppointmentStockProperties properties = new AppointmentStockProperties();
        properties.setRepairDelay(Duration.ofSeconds(3));
        properties.setRepairCheckTtl(Duration.ofSeconds(8));
        return new AppointmentStockRepairService(appointmentStockService, doctorScheduleSlotMapper, taskScheduler, properties);
    }

    private DoctorScheduleSlot openSlot(int remainingCapacity) {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setStatus("OPEN");
        slot.setRemainingCapacity(remainingCapacity);
        return slot;
    }
}
