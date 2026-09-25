package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.AppointmentService;
import com.healthy.appointment.service.AppointmentStockService;
import com.healthy.appointment.service.AppointmentStockRepairService;
import com.healthy.appointment.service.AppointmentWaitlistService;
import com.healthy.appointment.vo.AdminAppointmentVO;
import com.healthy.appointment.vo.PatientAppointmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.healthy.appointment.constant.Constant.APPOINTMENT_STATUS_BOOKED;
import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_OPEN;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.EMPTY;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.MISSING;
import static com.healthy.appointment.service.AppointmentStockService.PreDeductResult.SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final PatientMapper patientMapper;
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentStockService appointmentStockService;
    private final AppointmentStockRepairService appointmentStockRepairService;
    private final AppointmentWaitlistService appointmentWaitlistService;


    /** 创建最小预约记录，并同步扣减对应班次的剩余号源。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PatientAppointmentVO create(Long userId, AppointmentCreateDTO appointmentCreateDTO) {
        Long patientId = findEnabledPatientId(userId);
        PatientAppointmentVO existingByRequest = appointmentMapper.findByRequestIdAndPatientId(
                appointmentCreateDTO.getRequestId(), patientId);
        if (existingByRequest != null) {
            return existingByRequest;
        }
        Long slotId = appointmentCreateDTO.getScheduleSlotId();
        PatientAppointmentVO existingActiveAppointment = appointmentMapper
                .findActiveByPatientIdAndScheduleSlotId(patientId, slotId);
        if (existingActiveAppointment != null) {
            return existingActiveAppointment;
        }
        if (preDeductWithReload(slotId) != SUCCESS) {
            appointmentStockRepairService.triggerIfEmpty(slotId);
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        restoreRedisStockIfTransactionDoesNotCommit(slotId);
        DoctorScheduleSlot slot;
        try {
            slot = getAvailableSlot(slotId);
        } catch (BusinessException exception) {
            // Redis has already reserved one unit, but MySQL rejected the slot before the
            // conditional stock update. Schedule a delayed comparison so stale positive
            // Redis stock (for example Redis > 0 while MySQL = 0) can be removed safely.
            appointmentStockRepairService.triggerAfterMysqlStockReject(slotId);
            throw exception;
        }
        //校验影响行数，确保未抢到号时立刻抛出异常
        if (doctorScheduleSlotMapper.decreaseRemainingCapacity(slot.getId()) != 1) {
            // Redis 已预扣但 MySQL 最终条件校验失败。事务回滚回调会恢复 Redis；
            // 同时延迟校验是否仍存在 Redis 偏大，不在当前请求直接覆盖缓存。
            appointmentStockRepairService.triggerAfterMysqlStockReject(slotId);
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(UUID.randomUUID().toString().replace("-", ""));
        appointment.setRequestId(appointmentCreateDTO.getRequestId());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(slot.getDoctorId());
        appointment.setScheduleSlotId(slot.getId());
        appointment.setScheduleDate(slot.getScheduleDate());
        appointment.setStartTime(slot.getStartTime());
        appointment.setEndTime(slot.getEndTime());
        appointment.setStatus(APPOINTMENT_STATUS_BOOKED);
        try {
            if (appointmentMapper.insert(appointment) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            // 并发请求同时越过预查询时，由数据库唯一索引最终阻止重复预约；事务回滚后会回补 Redis 预扣库存。
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        PatientAppointmentVO result = appointmentMapper.findByIdAndPatientId(appointment.getId(), patientId);
        logAfterCommit(() -> log.info(
                "Appointment booked successfully: appointmentId={}, patientId={}, slotId={}",
                appointment.getId(), patientId, slotId));
        return result;
    }

    /** 查询当前登录患者的预约记录，最新创建的记录排在最前。 */
    @Override
    public List<PatientAppointmentVO> listMyAppointments(Long userId) {
        return appointmentMapper.listByPatientId(findEnabledPatientId(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long appointmentId) {
        Long patientId = findEnabledPatientId(userId);
        Appointment appointment = appointmentMapper.findEntityByIdAndPatientId(appointmentId, patientId);
        if (appointment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ensureCancellable(appointment);
        // Only one concurrent request can move BOOKED to CANCELLED and restore capacity.
        if (appointmentMapper.cancelBooked(appointmentId, patientId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        // A freed slot is reserved for the FIFO waitlist before it can ever enter public stock.
        if (appointmentWaitlistService.offerFirstWaiting(appointment.getScheduleSlotId())) {
            logAfterCommit(() -> log.info(
                    "Appointment cancelled successfully: appointmentId={}, patientId={}, slotId={}, stockDestination=WAITLIST",
                    appointmentId, patientId, appointment.getScheduleSlotId()));
            return;
        }
        if (doctorScheduleSlotMapper.increaseRemainingCapacity(appointment.getScheduleSlotId()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        appointmentStockService.restore(appointment.getScheduleSlotId());
        preDeductRedisStockIfTransactionDoesNotCommit(appointment.getScheduleSlotId());
        logAfterCommit(() -> log.info(
                "Appointment cancelled successfully: appointmentId={}, patientId={}, slotId={}, stockDestination=PUBLIC",
                appointmentId, patientId, appointment.getScheduleSlotId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long appointmentId) {
        Appointment appointment = appointmentMapper.findById(appointmentId);
        if (appointment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!APPOINTMENT_STATUS_BOOKED.equals(appointment.getStatus()) || !hasStarted(appointment)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        // Conditional update prevents concurrent completion/cancellation from crossing the state boundary twice.
        if (appointmentMapper.completeBooked(appointmentId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Override
    public PageResult<AdminAppointmentVO> pageForAdmin(int page, int pageSize) {
        validatePage(page, pageSize);
        IPage<AdminAppointmentVO> result = appointmentMapper.pageForAdmin(new Page<>(page, pageSize));
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    private Long findEnabledPatientId(Long userId) {
        Patient patient = patientMapper.selectOne(new LambdaQueryWrapper<Patient>()
                .select(Patient::getId)
                .eq(Patient::getUserId, userId)
                .eq(Patient::getStatus, 1)
                .last("LIMIT 1"));
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return patient.getId();
    }

    private DoctorScheduleSlot getAvailableSlot(Long slotId) {
        DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(slotId);
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate scheduleDate = slot.getScheduleDate();
        if (!SCHEDULE_STATUS_OPEN.equals(slot.getStatus())
                || slot.getRemainingCapacity() < 1
                || scheduleDate.isBefore(now.toLocalDate())
                || (scheduleDate.equals(now.toLocalDate()) && !slot.getEndTime().isAfter(now.toLocalTime()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return slot;
    }

    /**
     * Redis key 丢失时以 MySQL 为事实来源做一次 SETNX 重建，并且只重试一次 Lua 预扣。
     * SETNX 失败说明并发请求已经完成重建，直接重试 Lua 即可，绝不能覆盖库存。
     */
    private AppointmentStockService.PreDeductResult preDeductWithReload(Long slotId) {
        AppointmentStockService.PreDeductResult result = appointmentStockService.preDeduct(slotId);
        if (result != MISSING) {
            return result;
        }
        reloadStockIfMissing(slotId);
        result = appointmentStockService.preDeduct(slotId);
        if (result == MISSING) {
            // 已经做过一次 SETNX 和一次 Lua 重试，继续重试只会掩盖 Redis 异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return result;
    }

    private void reloadStockIfMissing(Long slotId) {
        DoctorScheduleSlot slot = getAvailableSlot(slotId);
        log.warn("Redis stock key missing; lazy load triggered: slotId={}, redisStock={}, mysqlStock={}",
                slotId, null, slot.getRemainingCapacity());
        appointmentStockService.initializeIfAbsent(slotId, slot.getRemainingCapacity());
    }

    private void logAfterCommit(Runnable loggingAction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            loggingAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                loggingAction.run();
            }
        });
    }

    private void restoreRedisStockIfTransactionDoesNotCommit(Long slotId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    appointmentStockService.restore(slotId);
                }
            }
        });
    }

    private void preDeductRedisStockIfTransactionDoesNotCommit(Long slotId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    // Redis was restored before commit, so rollback must reverse that restoration.
                    appointmentStockService.preDeduct(slotId);
                }
            }
        });
    }

    private void ensureCancellable(Appointment appointment) {
        if (!APPOINTMENT_STATUS_BOOKED.equals(appointment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (appointment.getScheduleDate().isBefore(now.toLocalDate())
                || (appointment.getScheduleDate().equals(now.toLocalDate())
                && !appointment.getStartTime().isAfter(now.toLocalTime()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private boolean hasStarted(Appointment appointment) {
        LocalDateTime now = LocalDateTime.now();
        return appointment.getScheduleDate().isBefore(now.toLocalDate())
                || (appointment.getScheduleDate().equals(now.toLocalDate())
                && !appointment.getStartTime().isAfter(now.toLocalTime()));
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
