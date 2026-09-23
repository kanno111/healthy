package com.healthy.appointment.service;

import com.healthy.appointment.vo.AdminDashboardSummaryVO;

import java.time.LocalDate;

public interface AdminDashboardService {
    AdminDashboardSummaryVO getSummary(LocalDate date);
}
