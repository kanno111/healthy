package com.healthy.appointment.domain.provider;

import lombok.Data;

@Data
public class DoctorSummary {
    private Long id;
    private Long userId;
    private String name;
    private Integer status;
    private Long departmentId;
    private String departmentName;
    private Integer departmentStatus;
}
