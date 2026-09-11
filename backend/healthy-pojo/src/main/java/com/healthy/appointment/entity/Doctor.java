package com.healthy.appointment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Doctor {
    private Long id;
    private String name;
    private Integer gender;
    private Long departmentId;
    private String doctorCode;
    private String title;
    private String introduction;
    private String avatarUrl;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
