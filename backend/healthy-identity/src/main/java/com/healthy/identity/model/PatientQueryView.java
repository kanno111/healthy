package com.healthy.identity.model;

import lombok.Data;

@Data
public class PatientQueryView {
    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String phone;
    private Integer status;
}
