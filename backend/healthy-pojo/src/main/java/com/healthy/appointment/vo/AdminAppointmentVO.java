package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 管理端查看预约订单所需的展示字段。 */
@Data
public class AdminAppointmentVO {
    private Long id;
    private String appointmentNo;
    private String patientName;
    private String doctorName;
    private String departmentName;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private LocalDateTime createdAt;
}
