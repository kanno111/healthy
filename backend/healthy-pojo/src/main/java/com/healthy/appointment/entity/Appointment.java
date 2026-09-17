package com.healthy.appointment.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class Appointment {
    private Long id;
    private String appointmentNo;
    private String requestId;
    private Long patientId;
    private Long doctorId;
    private Long scheduleSlotId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime createdAt;
}
