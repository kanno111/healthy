package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.service.impl.DoctorServiceImpl;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.appointment.vo.DoctorVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private DepartmentService departmentService;

    @Test
    void createSavesDoctorForEnabledDepartment() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);
        when(departmentService.getById(1L)).thenReturn(department(1));
        doAnswer(invocation -> {
            invocation.getArgument(0, Doctor.class).setId(10L);
            return 1;
        }).when(doctorMapper).insert(any(Doctor.class));

        Long id = service.create(request());

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorMapper).insert(captor.capture());
        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue().getName()).isEqualTo("doctor zhang");
        assertThat(captor.getValue().getSortOrder()).isZero();
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void createRejectsDisabledDepartment() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);
        when(departmentService.getById(1L)).thenReturn(department(0));

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(BusinessException.class);
    }

    @Test
    void createRejectsDuplicateDoctorCode() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);
        when(departmentService.getById(1L)).thenReturn(department(1));
        when(doctorMapper.exists(any())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request())).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatusRejectsInvalidValue() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);

        assertThatThrownBy(() -> service.updateStatus(1L, 2)).isInstanceOf(BusinessException.class);
    }

    @Test
    void pageUsesMybatisPlusPagination() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);
        DoctorVO doctor = new DoctorVO();
        doctor.setId(11L);
        Page<DoctorVO> resultPage = new Page<>(2, 10);
        resultPage.setTotal(21L);
        resultPage.setRecords(List.of(doctor));
        when(doctorMapper.page(any(IPage.class), eq("Zhang"), isNull(), eq(1L), eq(1), isNull()))
                .thenReturn(resultPage);

        var result = service.page(" Zhang ", 1L, 1, 2, 10);

        assertThat(result.records()).containsExactly(doctor);
        assertThat(result.total()).isEqualTo(21L);
        assertThat(result.totalPages()).isEqualTo(3L);
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    void pageVisibleRestrictsDoctorAndDepartmentStatus() {
        DoctorService service = new DoctorServiceImpl(doctorMapper, departmentService);
        when(doctorMapper.page(any(IPage.class), isNull(), eq("cardiology"), eq(1L), eq(1), eq(1)))
                .thenReturn(new Page<>(1, 10));

        service.pageVisible(1L, " cardiology ", 1, 10);

        verify(doctorMapper).page(any(IPage.class), isNull(), eq("cardiology"), eq(1L), eq(1), eq(1));
    }

    private DoctorSaveDTO request() {
        DoctorSaveDTO request = new DoctorSaveDTO();
        request.setName(" doctor zhang ");
        request.setGender(1);
        request.setDepartmentId(1L);
        request.setDoctorCode(" D1001 ");
        request.setTitle("attending");
        return request;
    }

    private DepartmentVO department(int status) {
        DepartmentVO department = new DepartmentVO();
        department.setId(1L);
        department.setStatus(status);
        return department;
    }
}
