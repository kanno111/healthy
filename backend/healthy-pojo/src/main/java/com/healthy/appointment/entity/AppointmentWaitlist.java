package com.healthy.appointment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_waitlist")
public class AppointmentWaitlist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private Long scheduleSlotId;
    private String status;
    private LocalDateTime offerExpireTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
