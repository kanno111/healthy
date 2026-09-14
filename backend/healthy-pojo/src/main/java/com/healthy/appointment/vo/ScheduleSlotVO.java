package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleSlotVO {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private LocalDate scheduleDate;
    private String sessionType;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer averageConsultationMinutes;
    private Integer totalCapacity;
    private Integer bookedCapacity;
    private Integer remainingCapacity;
    private String status;
}
