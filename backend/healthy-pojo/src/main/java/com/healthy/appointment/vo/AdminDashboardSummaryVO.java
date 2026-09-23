package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminDashboardSummaryVO {
    private LocalDate date;
    private Long scheduleSlotCount;
    private Long totalCapacity;
    private Long remainingCapacity;
    private Long activeAppointmentCount;
    private Long waitingCount;
    private Long overdueOfferedCount;
}
