package com.healthy.appointment.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DoctorScheduleSlot {
    private Long id;
    private Long doctorId;
    private LocalDate scheduleDate;
    private String sessionType;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer averageConsultationMinutes;
    private Integer totalCapacity;
    private Integer remainingCapacity;
    private Integer nextQueueNumber;
    private String status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
