package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.AdminAppointmentWaitlistService;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAppointmentWaitlistServiceImpl implements AdminAppointmentWaitlistService {
    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "WAITING", "OFFERED", "CONFIRMED", "EXPIRED", "CANCELLED");

    private final AppointmentWaitlistMapper appointmentWaitlistMapper;

    @Override
    public PageResult<AdminAppointmentWaitlistVO> page(
            String status, LocalDate scheduleDate, Long departmentId, Long doctorId,
            String patientKeyword, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String normalizedStatus = blankToNull(status);
        if (normalizedStatus != null && !SUPPORTED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        IPage<AdminAppointmentWaitlistVO> result = appointmentWaitlistMapper.pageForAdmin(
                new Page<>(page, pageSize), normalizedStatus, scheduleDate, departmentId, doctorId,
                blankToNull(patientKeyword));
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public AdminAppointmentWaitlistQueueVO getSlotQueue(Long scheduleSlotId) {
        AdminAppointmentWaitlistQueueVO queue = appointmentWaitlistMapper.findQueueSummary(scheduleSlotId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        queue.setOfferedCandidates(appointmentWaitlistMapper.listOfferedCandidates(scheduleSlotId));
        queue.setWaitingCandidates(appointmentWaitlistMapper.listWaitingCandidates(scheduleSlotId));
        return queue;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
