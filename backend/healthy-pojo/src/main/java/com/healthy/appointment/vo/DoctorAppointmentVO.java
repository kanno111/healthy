package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Minimal appointment information needed by the authenticated doctor. */
@Data
public class DoctorAppointmentVO {
    private Long id;
    private String appointmentNo;
    private String patientName;
    private String departmentName;
    private Long scheduleSlotId;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
}
