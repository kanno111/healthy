package com.healthy.doctor.model;

import lombok.Data;

import java.time.LocalDateTime;

/** Internal doctor view used by booking-facing directory queries. */
@Data
public class DoctorQueryView {
    private Long id;
    private Long userId;
    private String name;
    private Integer gender;
    private Long departmentId;
    private String departmentName;
    private Integer departmentStatus;
    private String doctorCode;
    private String title;
    private String introduction;
    private String avatarUrl;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
