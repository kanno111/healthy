package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.vo.PatientAppointmentVO;

import java.util.List;

public interface AppointmentService {
    PatientAppointmentVO create(Long userId, AppointmentCreateDTO appointmentCreateDTO);

    void cancel(Long userId, Long appointmentId);

    List<PatientAppointmentVO> listMyAppointments(Long userId);
}
