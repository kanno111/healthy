package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** 患者可以查看的出诊班次与剩余号源。 */
@Data
public class PatientScheduleSlotVO {
    private Long id;
    private LocalDate scheduleDate;
    private String sessionType;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalCapacity;
    private Integer remainingCapacity;
}
