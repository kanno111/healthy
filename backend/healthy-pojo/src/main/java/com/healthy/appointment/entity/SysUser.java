package com.healthy.appointment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String name;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
