package com.healthy.appointment.vo;

import lombok.Data;

/** 患者端医生列表与详情共用的公开信息。 */
@Data
public class PatientDoctorVO {
    private Long id;
    private String name;
    private Integer gender;
    private Long departmentId;
    private String departmentName;
    private String title;
    private String introduction;
    private String avatarUrl;
    private Boolean hasAvailableSlots;
}
