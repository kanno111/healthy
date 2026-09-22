package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.healthy.appointment.config.AppointmentWaitlistProperties;
import com.healthy.appointment.dto.AppointmentWaitlistCreateDTO;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.mq.WaitlistTimeoutMessagePublisher;
import com.healthy.appointment.service.AppointmentWaitlistService;
import com.healthy.appointment.service.AppointmentStockService;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.healthy.appointment.constant.Constant.APPOINTMENT_STATUS_BOOKED;
import static com.healthy.appointment.constant.Constant.SCHEDULE_STATUS_OPEN;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_CANCELLED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_CONFIRMED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_EXPIRED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_OFFERED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_WAITING;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentWaitlistServiceImpl implements AppointmentWaitlistService {
    private final PatientMapper patientMapper;
    private final DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentWaitlistMapper appointmentWaitlistMapper;
    private final AppointmentStockService appointmentStockService;
    private final AppointmentWaitlistProperties appointmentWaitlistProperties;
    private final WaitlistTimeoutMessagePublisher waitlistTimeoutMessagePublisher;

    @Override
    public PatientAppointmentWaitlistVO join(
            Long userId,
            AppointmentWaitlistCreateDTO appointmentWaitlistCreateDTO
    ) {
        Long patientId = findEnabledPatientId(userId);
        Long slotId = appointmentWaitlistCreateDTO.getScheduleSlotId();
        DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(slotId);
        ensureWaitlistable(slot);

        if (appointmentMapper.findActiveByPatientIdAndScheduleSlotId(patientId, slotId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        AppointmentWaitlist existing = findActiveWaitlist(patientId, slotId);
        if (existing != null) {
            return appointmentWaitlistMapper.findByIdAndPatientId(existing.getId(), patientId);
        }

        AppointmentWaitlist waitlist = new AppointmentWaitlist();
        waitlist.setPatientId(patientId);
        waitlist.setScheduleSlotId(slotId);
        waitlist.setStatus(WAITLIST_STATUS_WAITING);
        try {
            if (appointmentWaitlistMapper.insert(waitlist) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        } catch (DuplicateKeyException exception) {
            AppointmentWaitlist concurrentExisting = findActiveWaitlist(patientId, slotId);
            if (concurrentExisting != null) {
                return appointmentWaitlistMapper.findByIdAndPatientId(concurrentExisting.getId(), patientId);
            }
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        PatientAppointmentWaitlistVO result = appointmentWaitlistMapper.findByIdAndPatientId(waitlist.getId(), patientId);
        log.info("Joined appointment waitlist successfully: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}, offerExpireTime={}",
                waitlist.getId(), patientId, slotId, null, WAITLIST_STATUS_WAITING, null);
        return result;
    }

    @Override
    public List<PatientAppointmentWaitlistVO> listMyWaitlists(Long userId) {
        Long patientId = findEnabledPatientId(userId);
        return appointmentWaitlistMapper.listByPatientId(patientId);
    }

    @Override
    public void cancel(Long userId, Long waitlistId) {
        Long patientId = findEnabledPatientId(userId);
        AppointmentWaitlist waitlist = appointmentWaitlistMapper.selectOne(new LambdaQueryWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getPatientId, patientId));
        if (waitlist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!WAITLIST_STATUS_WAITING.equals(waitlist.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        int updated = appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getPatientId, patientId)
                .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_WAITING)
                .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_CANCELLED));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offerFirstWaiting(Long scheduleSlotId) {
        // Serialize allocation for one slot before locking a FIFO waitlist row. This avoids
        // InnoDB next-key-lock deadlocks when two cancellations release the same slot together.
        if (doctorScheduleSlotMapper.findByIdForUpdate(scheduleSlotId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        AppointmentWaitlist waiting = appointmentWaitlistMapper.selectFirstWaitingForUpdate(scheduleSlotId);
        if (waiting == null) {
            return false;
        }

        LocalDateTime offerExpireTime = LocalDateTime.now().plus(appointmentWaitlistProperties.getOfferDuration());
        int updated = appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waiting.getId())
                .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_WAITING)
                .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                .set(AppointmentWaitlist::getOfferExpireTime, offerExpireTime));
        if (updated != 1) {
            // selectFirstWaitingForUpdate has locked this row. A zero result would otherwise allow
            // a caller to return the reserved capacity to public stock while a waiter still exists.
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        logAfterCommit(() -> log.info(
                "Waitlist status changed: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}, offerExpireTime={}",
                waiting.getId(), waiting.getPatientId(), scheduleSlotId,
                WAITLIST_STATUS_WAITING, WAITLIST_STATUS_OFFERED, offerExpireTime));
        waitlistTimeoutMessagePublisher.publishAfterCommit(waiting.getId(), scheduleSlotId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long waitlistId) {
        Long patientId = findEnabledPatientId(userId);
        AppointmentWaitlist waitlist = appointmentWaitlistMapper.selectOne(new LambdaQueryWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getPatientId, patientId));
        if (waitlist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!WAITLIST_STATUS_OFFERED.equals(waitlist.getStatus())
                || waitlist.getOfferExpireTime() == null
                || !waitlist.getOfferExpireTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (appointmentMapper.findActiveByPatientIdAndScheduleSlotId(patientId, waitlist.getScheduleSlotId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        DoctorScheduleSlot slot = doctorScheduleSlotMapper.findById(waitlist.getScheduleSlotId());
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        int confirmed = appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getPatientId, patientId)
                .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                .gt(AppointmentWaitlist::getOfferExpireTime, now)
                .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_CONFIRMED));
        if (confirmed != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(UUID.randomUUID().toString().replace("-", ""));
        appointment.setRequestId("waitlist-confirm-" + waitlistId);
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
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        logAfterCommit(() -> log.info(
                "Waitlist status changed: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}, offerExpireTime={}",
                waitlistId, patientId, waitlist.getScheduleSlotId(),
                WAITLIST_STATUS_OFFERED, WAITLIST_STATUS_CONFIRMED, waitlist.getOfferExpireTime()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean expireOffered(Long waitlistId) {
        AppointmentWaitlist candidate = appointmentWaitlistMapper.selectById(waitlistId);
        if (candidate == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        int expired = appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                .le(AppointmentWaitlist::getOfferExpireTime, now)
                .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_EXPIRED));
        if (expired != 1) {
            return false;
        }
        logAfterCommit(() -> log.info(
                "Waitlist status changed: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}, offerExpireTime={}",
                candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId(),
                WAITLIST_STATUS_OFFERED, WAITLIST_STATUS_EXPIRED, candidate.getOfferExpireTime()));
        if (offerFirstWaiting(candidate.getScheduleSlotId())) {
            logAfterCommit(() -> log.info(
                    "Expired waitlist offer assigned to next patient: waitlistId={}, patientId={}, slotId={}",
                    candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId()));
        } else {
            releaseToPublicStock(candidate.getScheduleSlotId());
            logAfterCommit(() -> log.info(
                    "No waiting patient; capacity restored to public stock: waitlistId={}, patientId={}, slotId={}",
                    candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId()));
        }
        return true;
    }

    private AppointmentWaitlist findActiveWaitlist(Long patientId, Long slotId) {
        return appointmentWaitlistMapper.selectOne(new LambdaQueryWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getPatientId, patientId)
                .eq(AppointmentWaitlist::getScheduleSlotId, slotId)
                .in(AppointmentWaitlist::getStatus, "WAITING", "OFFERED"));
    }

    private void releaseToPublicStock(Long scheduleSlotId) {
        if (doctorScheduleSlotMapper.increaseRemainingCapacity(scheduleSlotId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        appointmentStockService.restore(scheduleSlotId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    // Redis was restored before commit; reverse it when this transaction rolls back.
                    appointmentStockService.preDeduct(scheduleSlotId);
                }
            }
        });
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

    private void ensureWaitlistable(DoctorScheduleSlot slot) {
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean alreadyEnded = slot.getScheduleDate().isBefore(now.toLocalDate())
                || (slot.getScheduleDate().equals(now.toLocalDate())
                && !slot.getEndTime().isAfter(now.toLocalTime()));
        if (!SCHEDULE_STATUS_OPEN.equals(slot.getStatus())
                || !Integer.valueOf(0).equals(slot.getRemainingCapacity())
                || alreadyEnded) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }
}
