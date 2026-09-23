package com.healthy.appointment.mapper;

import com.healthy.appointment.vo.AdminDashboardSummaryVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface AdminDashboardMapper {
    AdminDashboardSummaryVO getSummary(@Param("date") LocalDate date);
}
