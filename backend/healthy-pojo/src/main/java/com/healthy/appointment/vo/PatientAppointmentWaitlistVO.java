package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class PatientAppointmentWaitlistVO {
    private Long id;
    private Long scheduleSlotId;
    private String doctorName;
    private String departmentName;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime offerExpireTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
