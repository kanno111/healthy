package com.healthy.appointment.service;

import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.DoctorAppointmentVO;

import java.time.LocalDate;

public interface DoctorAppointmentService {
    PageResult<DoctorAppointmentVO> pageMine(
            Long userId, LocalDate scheduleDate, String status, int page, int pageSize);

    void complete(Long userId, Long appointmentId);
}
