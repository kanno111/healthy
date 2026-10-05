package com.healthy.appointment.domain.doctor;

import lombok.Data;

@Data
public class DoctorSummary {
    private Long id;
    private Long userId;
    private String name;
    private Integer gender;
    private Integer status;
    private Long departmentId;
    private String departmentName;
    private Integer departmentStatus;
    private String title;
    private String introduction;
    private String avatarUrl;
}
