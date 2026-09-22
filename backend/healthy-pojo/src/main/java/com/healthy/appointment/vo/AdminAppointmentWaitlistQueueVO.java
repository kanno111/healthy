package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AdminAppointmentWaitlistQueueVO {
    private Long scheduleSlotId;
    private String departmentName;
    private String doctorName;
    private LocalDate scheduleDate;
    private String sessionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalCapacity;
    private Integer remainingCapacity;
    private Long waitingCount;
    private List<AdminAppointmentWaitlistQueueItemVO> offeredCandidates;
    private List<AdminAppointmentWaitlistQueueItemVO> waitingCandidates;
}
