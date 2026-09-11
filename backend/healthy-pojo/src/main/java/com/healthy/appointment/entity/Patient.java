package com.healthy.appointment.entity;

import lombok.Data;

@Data
public class Patient {
    private Long id;
    private Long userId;
    private String realName;
    private Integer gender;
}
