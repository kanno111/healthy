package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAppointmentWaitlistQueueItemVO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String username;
    private String phone;
    private String status;
    private Long queuePosition;
    private LocalDateTime createdAt;
    private LocalDateTime offerExpireTime;
}
