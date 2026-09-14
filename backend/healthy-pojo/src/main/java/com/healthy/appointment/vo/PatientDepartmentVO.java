package com.healthy.appointment.vo;

import lombok.Data;

/** 患者端展示的启用科室信息。 */
@Data
public class PatientDepartmentVO {
    private Long id;
    private String name;
    private String description;
}
