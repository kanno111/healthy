package com.healthy.appointment.service;

import com.healthy.appointment.dto.ScheduleBatchDTO;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.service.impl.ScheduleSlotServiceImpl;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.appointment.vo.ScheduleBatchResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleSlotServiceImplTest {
    @Mock
    private DoctorService doctorService;

    @Mock
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;

    @Mock
    private AppointmentStockService appointmentStockService;

    @Test
    void batchCreateCreatesOneSlotForEachSession() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorService.getById(1L)).thenReturn(enabledDoctor());
        when(doctorScheduleSlotMapper.findOverlappingSlots(eq(1L), any())).thenReturn(List.of());

        ScheduleBatchResultVO result = service.batchCreate(request(date, LocalTime.of(9, 0), LocalTime.of(12, 0)));

        ArgumentCaptor<List<DoctorScheduleSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(doctorScheduleSlotMapper).batchInsert(captor.capture());
        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(captor.getValue()).extracting(DoctorScheduleSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0));
        assertThat(captor.getValue()).extracting(DoctorScheduleSlot::getEndTime)
                .containsExactly(LocalTime.of(12, 0));
        assertThat(captor.getValue()).allSatisfy(slot -> {
            assertThat(slot.getRemainingCapacity()).isEqualTo(8);
            assertThat(slot.getSessionType()).isEqualTo("MORNING");
            assertThat(slot.getSessionName()).isEqualTo("上午门诊");
            assertThat(slot.getAverageConsultationMinutes()).isEqualTo(15);
            assertThat(slot.getNextQueueNumber()).isEqualTo(1);
            assertThat(slot.getStatus()).isEqualTo("OPEN");
            assertThat(slot.getVersion()).isZero();
        });
    }

    @Test
    void batchCreateRejectsExistingOverlapWithoutWritingAnything() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        LocalDate date = LocalDate.now().plusDays(1);
        when(doctorService.getById(1L)).thenReturn(enabledDoctor());
        when(doctorScheduleSlotMapper.findOverlappingSlots(eq(1L), any())).thenReturn(List.of(new DoctorScheduleSlot()));

        assertThatThrownBy(() -> service.batchCreate(request(date, LocalTime.of(9, 0), LocalTime.of(12, 0))))
                .isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).batchInsert(any());
    }

    @Test
    void batchCreateRejectsOverlappingSessionsInSameRequest() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        LocalDate date = LocalDate.now().plusDays(1);
        ScheduleBatchDTO request = request(date, LocalTime.of(9, 0), LocalTime.of(12, 0));
        request.setSessions(List.of(session(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                session(LocalTime.of(10, 0), LocalTime.of(12, 30))));
        when(doctorService.getById(1L)).thenReturn(enabledDoctor());

        assertThatThrownBy(() -> service.batchCreate(request)).isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).findOverlappingSlots(any(), any());
        verify(doctorScheduleSlotMapper, never()).batchInsert(any());
    }

    @Test
    void updateCapacityUsesPositiveDeltaInsteadOfOverwritingConcurrentRedisDeduction() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot(10L, 6, 2));
        when(doctorScheduleSlotMapper.updateCapacity(10L, 15, 2)).thenReturn(1);

        service.updateCapacity(10L, 15);

        verify(appointmentStockService).adjustByDeltaIfPresent(10L, 5);
        verify(appointmentStockService, never()).initialize(10L, 11);
    }

    @Test
    void updateCapacityUsesNegativeDeltaInsteadOfOverwritingConcurrentRedisDeduction() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        // 已预约 4 个，缩容至 8 仍合法；Redis 仅原子减 2，不会 SET 覆盖并发 DECR。
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot(10L, 6, 2));
        when(doctorScheduleSlotMapper.updateCapacity(10L, 8, 2)).thenReturn(1);

        service.updateCapacity(10L, 8);

        verify(appointmentStockService).adjustByDeltaIfPresent(10L, -2);
    }

    @Test
    void updateCapacityLeavesMissingRedisKeyForLazyReload() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(doctorService, doctorScheduleSlotMapper, appointmentStockService);
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(slot(10L, 6, 2));
        when(doctorScheduleSlotMapper.updateCapacity(10L, 12, 2)).thenReturn(1);
        // false 表示 Lua 发现 Redis key 缺失，服务层不 SET 新值。
        when(appointmentStockService.adjustByDeltaIfPresent(10L, 2)).thenReturn(false);

        service.updateCapacity(10L, 12);

        verify(appointmentStockService).adjustByDeltaIfPresent(10L, 2);
        verify(appointmentStockService, never()).initialize(10L, 8);
        verify(appointmentStockService, never()).initializeIfAbsent(anyLong(), anyInt());
    }

    @Test
    void updateStatusRejectsReopeningEndedSlot() {
        ScheduleSlotService service = new ScheduleSlotServiceImpl(
                doctorService, doctorScheduleSlotMapper, appointmentStockService);
        DoctorScheduleSlot endedSlot = slot(10L, 2, 3);
        endedSlot.setScheduleDate(LocalDate.now().minusDays(1));
        endedSlot.setEndTime(LocalTime.of(12, 0));
        endedSlot.setStatus("CLOSED");
        when(doctorScheduleSlotMapper.findById(10L)).thenReturn(endedSlot);

        assertThatThrownBy(() -> service.updateStatus(10L, "OPEN"))
                .isInstanceOf(BusinessException.class);

        verify(doctorScheduleSlotMapper, never()).updateStatus(anyLong(), any(), anyInt());
        verify(appointmentStockService, never()).initialize(anyLong(), anyInt());
    }

    private ScheduleBatchDTO request(LocalDate date, LocalTime startTime, LocalTime endTime) {
        ScheduleBatchDTO request = new ScheduleBatchDTO();
        request.setDoctorId(1L);
        request.setStartDate(date);
        request.setEndDate(date);
        request.setWeekdays(List.of(date.getDayOfWeek().getValue()));
        request.setSessions(List.of(session(startTime, endTime)));
        return request;
    }

    private ScheduleBatchDTO.Session session(LocalTime startTime, LocalTime endTime) {
        ScheduleBatchDTO.Session session = new ScheduleBatchDTO.Session();
        session.setSessionType("MORNING");
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCapacity(8);
        session.setAverageConsultationMinutes(15);
        return session;
    }

    private DoctorVO enabledDoctor() {
        DoctorVO doctor = new DoctorVO();
        doctor.setId(1L);
        doctor.setStatus(1);
        return doctor;
    }

    private DoctorScheduleSlot slot(Long id, int remainingCapacity, int version) {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setId(id);
        slot.setTotalCapacity(10);
        slot.setRemainingCapacity(remainingCapacity);
        slot.setVersion(version);
        return slot;
    }
}
