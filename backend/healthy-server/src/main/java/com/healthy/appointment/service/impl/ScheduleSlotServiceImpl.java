package com.healthy.appointment.service.impl;

import com.healthy.appointment.dto.ScheduleBatchDTO;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.enumeration.ScheduleSessionType;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.service.AppointmentStockService;
import com.healthy.appointment.service.DoctorService;
import com.healthy.appointment.service.ScheduleSlotService;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.appointment.vo.ScheduleBatchResultVO;
import com.healthy.appointment.vo.ScheduleSlotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.healthy.appointment.constant.Constant.MAX_SCHEDULE_DAYS;
import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_CLOSED;
import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_OPEN;

@Service
@RequiredArgsConstructor
public class ScheduleSlotServiceImpl implements ScheduleSlotService {
    private final DoctorService doctorService;
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    private final AppointmentStockService appointmentStockService;

    /**
     * 先完整校验本次请求和既有排班，再一次性入库，避免批量请求只成功一部分。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleBatchResultVO batchCreate(ScheduleBatchDTO scheduleBatchDTO) {
        validateDoctor(scheduleBatchDTO.getDoctorId());
        validateDateRange(scheduleBatchDTO);
        validateWeekdays(scheduleBatchDTO.getWeekdays());

        List<DoctorScheduleSlot> slots = generateSlots(scheduleBatchDTO);
        validateNoInternalOverlap(slots);
        if (!doctorScheduleSlotMapper.findOverlappingSlots(scheduleBatchDTO.getDoctorId(), slots).isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        doctorScheduleSlotMapper.batchInsert(slots);
        runAfterCommit(() -> slots.forEach(slot ->
                appointmentStockService.initialize(slot.getId(), slot.getRemainingCapacity())));
        return new ScheduleBatchResultVO(slots.size(), slots.stream().map(this::toVO).toList());
    }

    @Override
    public List<ScheduleSlotVO> list(Long doctorId, LocalDate startDate, LocalDate endDate) {
        validateQueryDateRange(startDate, endDate);
        if (doctorId != null) {
            doctorService.getById(doctorId);
        }
        return doctorScheduleSlotMapper.list(doctorId, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        String normalizedStatus = normalizeStatus(status);
        DoctorScheduleSlot slot = getSlotById(id);
        if (doctorScheduleSlotMapper.updateStatus(id, normalizedStatus, slot.getVersion()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (SCHEDULE_STATUS_OPEN.equals(normalizedStatus)) {
            runAfterCommit(() -> appointmentStockService.initialize(id, slot.getRemainingCapacity()));
        } else {
            runAfterCommit(() -> appointmentStockService.remove(id));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCapacity(Long id, Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        DoctorScheduleSlot slot = getSlotById(id);
        int bookedCapacity = slot.getTotalCapacity() - slot.getRemainingCapacity();
        if (capacity < bookedCapacity) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (doctorScheduleSlotMapper.updateCapacity(id, capacity, slot.getVersion()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        int delta = capacity - slot.getTotalCapacity();
        if (delta != 0) {
            // INCRBY 与患者的 Lua DECR 可交换，避免提交后的 SET 覆盖并发扣减。
            // Key 缺失时不创建，后续挂号会按 MISSING 路径以 MySQL 数据懒加载。
            runAfterCommit(() -> appointmentStockService.adjustByDeltaIfPresent(id, delta));
        }
    }

    private void validateDoctor(Long doctorId) {
        DoctorVO doctor = doctorService.getById(doctorId);
        if (doctor.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void validateDateRange(ScheduleBatchDTO request) {
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)
                || request.getEndDate().isBefore(request.getStartDate())
                || ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1 > MAX_SCHEDULE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateWeekdays(List<Integer> weekdays) {
        Set<Integer> uniqueWeekdays = new HashSet<>(weekdays);
        if (uniqueWeekdays.size() != weekdays.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private List<DoctorScheduleSlot> generateSlots(ScheduleBatchDTO request) {
        List<DoctorScheduleSlot> slots = new ArrayList<>();
        Set<Integer> weekdays = new HashSet<>(request.getWeekdays());
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            if (!weekdays.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            for (ScheduleBatchDTO.Session session : request.getSessions()) {
                ScheduleSessionType sessionType = validateSession(session);
                slots.add(newSlot(request.getDoctorId(), date, session, sessionType));
            }
        }
        if (slots.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return slots;
    }

    private ScheduleSessionType validateSession(ScheduleBatchDTO.Session session) {
        if (!session.getStartTime().isBefore(session.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        long sessionMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
        if ((long) session.getCapacity() * session.getAverageConsultationMinutes() > sessionMinutes) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        ScheduleSessionType sessionType;
        try {
            sessionType = ScheduleSessionType.valueOf(session.getSessionType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (sessionType == ScheduleSessionType.OTHER
                && (session.getCustomSessionName() == null || session.getCustomSessionName().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return sessionType;
    }

    private DoctorScheduleSlot newSlot(Long doctorId, LocalDate date, ScheduleBatchDTO.Session session, ScheduleSessionType sessionType) {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setDoctorId(doctorId);
        slot.setScheduleDate(date);
        slot.setSessionType(sessionType.name());
        slot.setSessionName(sessionType == ScheduleSessionType.OTHER
                ? session.getCustomSessionName().trim()
                : sessionType.fixedName());
        slot.setStartTime(session.getStartTime());
        slot.setEndTime(session.getEndTime());
        slot.setAverageConsultationMinutes(session.getAverageConsultationMinutes());
        slot.setTotalCapacity(session.getCapacity());
        slot.setRemainingCapacity(session.getCapacity());
        slot.setNextQueueNumber(1);
        slot.setStatus(SCHEDULE_STATUS_OPEN);
        slot.setVersion(0);
        return slot;
    }

    private void validateNoInternalOverlap(List<DoctorScheduleSlot> slots) {
        List<DoctorScheduleSlot> sortedSlots = slots.stream()
                .sorted(Comparator.comparing(DoctorScheduleSlot::getScheduleDate)
                        .thenComparing(DoctorScheduleSlot::getStartTime))
                .toList();
        for (int index = 1; index < sortedSlots.size(); index++) {
            DoctorScheduleSlot previous = sortedSlots.get(index - 1);
            DoctorScheduleSlot current = sortedSlots.get(index);
            if (previous.getScheduleDate().equals(current.getScheduleDate())
                    && previous.getEndTime().isAfter(current.getStartTime())) {
                throw new BusinessException(ErrorCode.CONFLICT);
            }
        }
    }

    private ScheduleSlotVO toVO(DoctorScheduleSlot slot) {
        ScheduleSlotVO vo = new ScheduleSlotVO();
        vo.setId(slot.getId());
        vo.setDoctorId(slot.getDoctorId());
        vo.setScheduleDate(slot.getScheduleDate());
        vo.setSessionType(slot.getSessionType());
        vo.setSessionName(slot.getSessionName());
        vo.setStartTime(slot.getStartTime());
        vo.setEndTime(slot.getEndTime());
        vo.setAverageConsultationMinutes(slot.getAverageConsultationMinutes());
        vo.setTotalCapacity(slot.getTotalCapacity());
        vo.setBookedCapacity(slot.getTotalCapacity() - slot.getRemainingCapacity());
        vo.setRemainingCapacity(slot.getRemainingCapacity());
        vo.setStatus(slot.getStatus());
        return vo;
    }

    private void validateQueryDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_SCHEDULE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private DoctorScheduleSlot getSlotById(Long id) {
        DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(id);
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return slot;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalizedStatus = status.trim().toUpperCase();
        if (!SCHEDULE_STATUS_OPEN.equals(normalizedStatus) && !SCHEDULE_STATUS_CLOSED.equals(normalizedStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalizedStatus;
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
