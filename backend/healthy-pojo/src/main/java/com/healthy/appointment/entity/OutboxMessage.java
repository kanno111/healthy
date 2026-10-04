package com.healthy.appointment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_outbox")
public class OutboxMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String exchangeName;
    private String routingKey;
    private LocalDateTime scheduledAt;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime processingStartedAt;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
