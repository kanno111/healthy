package com.healthy.appointment.service;

import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.PatientResourceMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.impl.PatientResourceServiceImpl;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.appointment.vo.DoctorVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientResourceServiceImplTest {
    @Mock
    private DepartmentService departmentService;
    @Mock
    private DoctorService doctorService;
    @Mock
    private PatientResourceMapper patientResourceMapper;

    @Test
    void pageDoctorsAddsAvailabilityToCurrentPage() {
        PatientResourceService service = createService();
        DoctorVO doctor = doctor(1L, 1);
        doctor.setName("张医生");
        when(doctorService.pageVisible(1L, "  张医生  ", 1, 10))
                .thenReturn(PageResult.of(List.of(doctor), 1, 1, 10));
        LocalDate today = LocalDate.now();
        when(patientResourceMapper.listDoctorIdsWithAvailableSlots(
                any(), any(), any(), any(), any(LocalTime.class))).thenReturn(List.of());

        var result = service.pageDoctors(1L, "  张医生  ", 1, 10);

        assertThat(result.records()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
        verify(doctorService).pageVisible(1L, "  张医生  ", 1, 10);
    }

    @Test
    void getDoctorRejectsInvisibleDoctor() {
        PatientResourceService service = createService();
        DoctorVO doctor = doctor(1L, 0);
        when(doctorService.getById(1L)).thenReturn(doctor);
        when(departmentService.getById(1L)).thenReturn(enabledDepartment(1L));

        assertThatThrownBy(() -> service.getDoctorById(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void listScheduleSlotsRejectsPastDate() {
        PatientResourceService service = createService();
        when(doctorService.getById(1L)).thenReturn(doctor(1L, 1));
        when(departmentService.getById(1L)).thenReturn(enabledDepartment(1L));

        assertThatThrownBy(() -> service.listScheduleSlots(1L, LocalDate.now().minusDays(1), LocalDate.now()))
                .isInstanceOf(BusinessException.class);

        verify(patientResourceMapper, never()).listScheduleSlots(
                any(), any(), any(), any(), any(LocalTime.class)
        );
    }

    private PatientResourceService createService() {
        return new PatientResourceServiceImpl(departmentService, doctorService, patientResourceMapper);
    }

    private DepartmentVO enabledDepartment(Long id) {
        DepartmentVO department = new DepartmentVO();
        department.setId(id);
        department.setStatus(1);
        return department;
    }

    private DoctorVO doctor(Long id, Integer status) {
        DoctorVO doctor = new DoctorVO();
        doctor.setId(id);
        doctor.setDepartmentId(1L);
        doctor.setStatus(status);
        return doctor;
    }
}
