package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.healthy.appointment.config.AppointmentWaitlistProperties;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_OFFERED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_EXPIRED;

/**
 * Low-frequency recovery coordinator. The scan itself has no surrounding transaction so every
 * candidate is handled through {@link AppointmentWaitlistService#expireOffered(Long)} in its own
 * transaction. One broken record therefore cannot roll back the rest of the batch.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentWaitlistExpiryRecoveryService {
    private static final int MAX_BATCH_SIZE = 1000;

    private final AppointmentWaitlistMapper appointmentWaitlistMapper;
    private final AppointmentWaitlistService appointmentWaitlistService;
    private final AppointmentWaitlistProperties appointmentWaitlistProperties;

    public RecoveryResult recoverDueOffers() {
        int batchSize = Math.max(1,
                Math.min(appointmentWaitlistProperties.getRecoveryBatchSize(), MAX_BATCH_SIZE));
        List<AppointmentWaitlist> candidates = appointmentWaitlistMapper.selectList(
                new LambdaQueryWrapper<AppointmentWaitlist>()
                        .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                        .le(AppointmentWaitlist::getOfferExpireTime, LocalDateTime.now())
                        .orderByAsc(AppointmentWaitlist::getOfferExpireTime)
                        .orderByAsc(AppointmentWaitlist::getId)
                        .last("LIMIT " + batchSize));

        int expired = 0;
        int skipped = 0;
        int failed = 0;
        for (AppointmentWaitlist candidate : candidates) {
            log.warn("Scheduled scan found stale OFFERED waitlist: waitlistId={}, patientId={}, slotId={}, oldStatus={}, offerExpireTime={}",
                    candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId(),
                    candidate.getStatus(), candidate.getOfferExpireTime());
            try {
                if (appointmentWaitlistService.expireOffered(candidate.getId())) {
                    expired++;
                    log.info("Scheduled compensation processed successfully: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}, offerExpireTime={}",
                            candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId(),
                            WAITLIST_STATUS_OFFERED, WAITLIST_STATUS_EXPIRED, candidate.getOfferExpireTime());
                } else {
                    skipped++;
                    log.warn("Scheduled compensation skipped because business state already changed: waitlistId={}, patientId={}, slotId={}, affectedRows=0",
                            candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId());
                }
            } catch (RuntimeException exception) {
                failed++;
                log.error("Waitlist expiry recovery candidate failed: waitlistId={}, reason={}",
                        candidate.getId(), exception.getMessage(), exception);
            }
        }
        return new RecoveryResult(candidates.size(), expired, skipped, failed);
    }

    public RecoveryResult recoverExpiredWaiting() {
        int batchSize = Math.max(1,
                Math.min(appointmentWaitlistProperties.getRecoveryBatchSize(), MAX_BATCH_SIZE));
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentWaitlist> candidates =
                appointmentWaitlistMapper.selectExpiredWaiting(now, batchSize);

        int expired = 0;
        int skipped = 0;
        int failed = 0;
        for (AppointmentWaitlist candidate : candidates) {
            log.warn("Scheduled scan found WAITING waitlist after schedule ended: waitlistId={}, patientId={}, slotId={}, oldStatus={}",
                    candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId(),
                    candidate.getStatus());
            try {
                if (appointmentWaitlistService.expireWaiting(candidate.getId())) {
                    expired++;
                    log.info("Scheduled compensation processed successfully: waitlistId={}, patientId={}, slotId={}, oldStatus={}, newStatus={}",
                            candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId(),
                            candidate.getStatus(), WAITLIST_STATUS_EXPIRED);
                } else {
                    skipped++;
                    log.warn("Scheduled WAITING expiry skipped because business state already changed: waitlistId={}, patientId={}, slotId={}, affectedRows=0",
                            candidate.getId(), candidate.getPatientId(), candidate.getScheduleSlotId());
                }
            } catch (RuntimeException exception) {
                failed++;
                log.error("WAITING waitlist expiry recovery candidate failed: waitlistId={}, reason={}",
                        candidate.getId(), exception.getMessage(), exception);
            }
        }
        return new RecoveryResult(candidates.size(), expired, skipped, failed);
    }

    public record RecoveryResult(int scanned, int expired, int skipped, int failed) {
    }
}
