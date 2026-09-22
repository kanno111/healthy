package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AdminAppointmentWaitlistVO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String username;
    private String phone;
    private Long scheduleSlotId;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime offerExpireTime;
    /** Derived from updated_at only while the current status is CONFIRMED. */
    private LocalDateTime confirmedAt;
    /** Derived from updated_at only while the current status is CANCELLED. */
    private LocalDateTime cancelledAt;
}
