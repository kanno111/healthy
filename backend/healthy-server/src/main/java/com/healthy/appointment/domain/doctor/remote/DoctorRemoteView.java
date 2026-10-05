package com.healthy.appointment.domain.doctor.remote;

import lombok.Data;

/** Response model owned by the healthy-doctor internal HTTP contract. */
@Data
public class DoctorRemoteView {
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
