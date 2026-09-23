package com.healthy.appointment.service;

import com.healthy.appointment.mapper.AdminDashboardMapper;
import com.healthy.appointment.service.impl.AdminDashboardServiceImpl;
import com.healthy.appointment.vo.AdminDashboardSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {
    @Mock
    private AdminDashboardMapper adminDashboardMapper;

    @Test
    void returnsSummaryForRequestedDate() {
        LocalDate date = LocalDate.of(2026, 9, 23);
        AdminDashboardSummaryVO expected = new AdminDashboardSummaryVO();
        expected.setDate(date);
        when(adminDashboardMapper.getSummary(date)).thenReturn(expected);

        AdminDashboardSummaryVO result = new AdminDashboardServiceImpl(adminDashboardMapper).getSummary(date);

        assertThat(result).isSameAs(expected);
        verify(adminDashboardMapper).getSummary(date);
    }
}
