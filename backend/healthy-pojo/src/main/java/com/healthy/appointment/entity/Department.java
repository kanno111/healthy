package com.healthy.appointment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Department {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
