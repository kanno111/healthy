package com.healthy.appointment.mapper;

import com.healthy.appointment.vo.AdminDashboardSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminDashboardMapperIntegrationTest {
    @Autowired
    private AdminDashboardMapper adminDashboardMapper;

    @Test
    void aggregatesRealDashboardDataWithoutNullCounters() {
        LocalDate date = LocalDate.now();

        AdminDashboardSummaryVO summary = adminDashboardMapper.getSummary(date);

        assertThat(summary).isNotNull();
        assertThat(summary.getDate()).isEqualTo(date);
        assertThat(summary.getScheduleSlotCount()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.getTotalCapacity()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.getRemainingCapacity()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.getActiveAppointmentCount()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.getWaitingCount()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(summary.getOverdueOfferedCount()).isNotNull().isGreaterThanOrEqualTo(0L);
    }
}
