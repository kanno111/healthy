package com.healthy.appointment.domain.identity;

import lombok.Data;

@Data
public class PatientSummary {
    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String phone;
    private Integer status;
}
