package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.vo.PatientAppointmentVO;

import java.util.List;

public interface AppointmentService {
    PatientAppointmentVO create(Long userId, AppointmentCreateDTO appointmentCreateDTO);

    List<PatientAppointmentVO> listMyAppointments(Long userId);
}
