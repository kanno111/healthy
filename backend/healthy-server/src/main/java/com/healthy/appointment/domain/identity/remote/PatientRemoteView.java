package com.healthy.appointment.domain.identity.remote;

import lombok.Data;

@Data
public class PatientRemoteView {
    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String phone;
    private Integer status;
}
