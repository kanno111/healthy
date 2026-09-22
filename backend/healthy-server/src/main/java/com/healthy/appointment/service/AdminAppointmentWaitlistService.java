package com.healthy.appointment.service;

import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistVO;

import java.time.LocalDate;

public interface AdminAppointmentWaitlistService {
    PageResult<AdminAppointmentWaitlistVO> page(
            String status, LocalDate scheduleDate, Long departmentId, Long doctorId,
            String patientKeyword, int page, int pageSize);

    AdminAppointmentWaitlistQueueVO getSlotQueue(Long scheduleSlotId);
}
