package com.healthy.appointment.service.impl;

import com.healthy.appointment.mapper.AdminDashboardMapper;
import com.healthy.appointment.service.AdminDashboardService;
import com.healthy.appointment.vo.AdminDashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final AdminDashboardMapper adminDashboardMapper;

    @Override
    public AdminDashboardSummaryVO getSummary(LocalDate date) {
        return adminDashboardMapper.getSummary(date == null ? LocalDate.now() : date);
    }
}
