package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentMapper;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.DoctorAppointmentService;
import com.healthy.appointment.vo.DoctorAppointmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorAppointmentServiceImpl implements DoctorAppointmentService {
    private static final Set<String> SUPPORTED_STATUSES = Set.of("BOOKED", "COMPLETED", "CANCELLED");

    private final DoctorMapper doctorMapper;
    private final AppointmentMapper appointmentMapper;

    @Override
    public PageResult<DoctorAppointmentVO> pageMine(
            Long userId, LocalDate scheduleDate, String status, int page, int pageSize) {
        validatePage(page, pageSize);
        LocalDate effectiveDate = scheduleDate == null ? LocalDate.now() : scheduleDate;
        String normalizedStatus = normalizeStatus(status);
        try {
            Doctor doctor = requireCurrentDoctor(userId);
            IPage<DoctorAppointmentVO> result = appointmentMapper.pageForDoctor(
                    new Page<>(page, pageSize), doctor.getId(), effectiveDate, normalizedStatus);
            log.info("Doctor queried own appointments: userId={}, doctorId={}, scheduleDate={}, status={}, page={}, pageSize={}, total={}",
                    userId, doctor.getId(), effectiveDate, normalizedStatus, page, pageSize, result.getTotal());
            return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
        } catch (DataAccessException exception) {
            log.error("Doctor appointment query failed: userId={}, scheduleDate={}, status={}, page={}, pageSize={}",
                    userId, effectiveDate, normalizedStatus, page, pageSize, exception);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long userId, Long appointmentId) {
        try {
            Doctor doctor = requireCurrentDoctor(userId);
            int affectedRows = appointmentMapper.completeBookedByDoctor(appointmentId, doctor.getId());
            if (affectedRows == 1) {
                logAfterCommit(() -> log.info(
                        "Doctor completed appointment successfully: appointmentId={}, doctorId={}, oldStatus=BOOKED, newStatus=COMPLETED",
                        appointmentId, doctor.getId()));
                return;
            }

            Appointment appointment = appointmentMapper.findById(appointmentId);
            if (appointment != null && !doctor.getId().equals(appointment.getDoctorId())) {
                log.warn("Doctor attempted to complete another doctor's appointment: appointmentId={}, doctorId={}, ownerDoctorId={}, affectedRows=0",
                        appointmentId, doctor.getId(), appointment.getDoctorId());
            } else {
                log.warn("Doctor appointment completion skipped because state changed or appointment does not exist: appointmentId={}, doctorId={}, currentStatus={}, affectedRows=0",
                        appointmentId, doctor.getId(), appointment == null ? null : appointment.getStatus());
            }
            throw new BusinessException(ErrorCode.CONFLICT);
        } catch (DataAccessException exception) {
            log.error("Doctor appointment completion failed: userId={}, appointmentId={}",
                    userId, appointmentId, exception);
            throw exception;
        }
    }

    private Doctor requireCurrentDoctor(Long userId) {
        Doctor doctor = doctorMapper.findEnabledByUserId(userId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return doctor;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATUSES.contains(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
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
}
