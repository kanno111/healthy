package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class PatientAppointmentVO {
    private Long id;
    private String appointmentNo;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private Long scheduleSlotId;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime createdAt;
}
