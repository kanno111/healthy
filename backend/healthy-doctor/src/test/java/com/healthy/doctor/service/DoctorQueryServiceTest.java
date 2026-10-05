package com.healthy.doctor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.doctor.mapper.DoctorQueryMapper;
import com.healthy.doctor.model.DoctorQueryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorQueryServiceTest {
    @Mock
    private DoctorQueryMapper doctorQueryMapper;

    @InjectMocks
    private DoctorQueryService doctorQueryService;

    @Test
    void pagesDoctorsWithNormalizedName() {
        DoctorVO doctor = new DoctorVO();
        doctor.setId(8L);
        Page<DoctorVO> resultPage = new Page<>(2, 10);
        resultPage.setTotal(21);
        resultPage.setRecords(List.of(doctor));
        when(doctorQueryMapper.page(any(), eq("Zhang"), isNull(), eq(1L), eq(1), isNull()))
                .thenReturn(resultPage);

        var result = doctorQueryService.page("  Zhang  ", 1L, 1, 2, 10);

        assertThat(result.records()).containsExactly(doctor);
        assertThat(result.total()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void visiblePageRestrictsDoctorAndDepartmentStatus() {
        when(doctorQueryMapper.page(any(), isNull(), eq("cardiology"), eq(1L), eq(1), eq(1)))
                .thenReturn(new Page<>(1, 10));

        doctorQueryService.pageVisible(1L, " cardiology ", 1, 10);

        verify(doctorQueryMapper).page(any(), isNull(), eq("cardiology"), eq(1L), eq(1), eq(1));
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> doctorQueryService.page(null, null, null, 0, 10))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void reportsMissingDoctor() {
        when(doctorQueryMapper.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> doctorQueryService.getById(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void reportsForbiddenWhenUserHasNoEnabledDoctor() {
        when(doctorQueryMapper.findEnabledByUserId(99L)).thenReturn(null);

        assertThatThrownBy(() -> doctorQueryService.getEnabledByUserId(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void deduplicatesBatchIds() {
        DoctorQueryView doctor = new DoctorQueryView();
        doctor.setId(8L);
        when(doctorQueryMapper.listByIds(List.of(8L, 9L))).thenReturn(List.of(doctor));

        assertThat(doctorQueryService.listByIds(List.of(8L, 8L, 9L))).containsExactly(doctor);
        verify(doctorQueryMapper).listByIds(List.of(8L, 9L));
    }

    @Test
    void skipsMapperForEmptyBatch() {
        assertThat(doctorQueryService.listByIds(List.of())).isEmpty();
        verify(doctorQueryMapper, never()).listByIds(any());
    }

    @Test
    void rejectsOversizedBatch() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> doctorQueryService.listByIds(ids))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
