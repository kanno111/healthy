package com.healthy.doctor.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.doctor.mapper.DepartmentCommandMapper;
import com.healthy.doctor.mapper.DoctorCommandMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorCommandServiceTest {
    @BeforeAll
    static void initializeDoctorTableInfo() {
        if (TableInfoHelper.getTableInfo(Doctor.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test-mapper"),
                    Doctor.class
            );
        }
    }

    @Mock
    private DoctorCommandMapper doctorCommandMapper;

    @Mock
    private DepartmentCommandMapper departmentCommandMapper;

    @InjectMocks
    private DoctorCommandService doctorCommandService;

    @Test
    void createsEnabledDoctorWithNormalizedFields() {
        when(departmentCommandMapper.selectById(4L)).thenReturn(enabledDepartment(4L));
        when(doctorCommandMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            invocation.getArgument(0, Doctor.class).setId(10L);
            return 1;
        }).when(doctorCommandMapper).insert(any(Doctor.class));

        Long id = doctorCommandService.create(saveRequest("  Zhang San  ", 1, 4L, " D-001 ", null));

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorCommandMapper).insert(captor.capture());
        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue())
                .extracting(
                        Doctor::getName,
                        Doctor::getGender,
                        Doctor::getDepartmentId,
                        Doctor::getDoctorCode,
                        Doctor::getTitle,
                        Doctor::getSortOrder,
                        Doctor::getStatus
                )
                .containsExactly("Zhang San", 1, 4L, "D-001", "Attending physician", 0, 1);
    }

    @Test
    void rejectsDisabledDepartmentBeforeCheckingDoctorCode() {
        when(departmentCommandMapper.selectById(4L)).thenReturn(disabledDepartment(4L));

        assertThatThrownBy(() -> doctorCommandService.create(saveRequest("Zhang San", 1, 4L, "D-001", 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        verify(doctorCommandMapper, never()).exists(any());
        verify(doctorCommandMapper, never()).insert(any(Doctor.class));
    }

    @Test
    void rejectsDuplicateDoctorCodeOnCreate() {
        when(departmentCommandMapper.selectById(4L)).thenReturn(enabledDepartment(4L));
        when(doctorCommandMapper.exists(any())).thenReturn(true);

        assertThatThrownBy(() -> doctorCommandService.create(saveRequest("Zhang San", 1, 4L, "D-001", 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        verify(doctorCommandMapper, never()).insert(any(Doctor.class));
    }

    @Test
    void updatesExistingDoctor() {
        when(doctorCommandMapper.selectById(8L)).thenReturn(doctor(8L));
        when(departmentCommandMapper.selectById(5L)).thenReturn(enabledDepartment(5L));
        when(doctorCommandMapper.exists(any())).thenReturn(false);

        doctorCommandService.update(8L, saveRequest("  Li Si  ", 2, 5L, " D-002 ", 3));

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorCommandMapper).updateById(captor.capture());
        assertThat(captor.getValue())
                .extracting(
                        Doctor::getId,
                        Doctor::getName,
                        Doctor::getGender,
                        Doctor::getDepartmentId,
                        Doctor::getDoctorCode,
                        Doctor::getSortOrder
                )
                .containsExactly(8L, "Li Si", 2, 5L, "D-002", 3);
    }

    @Test
    void reportsMissingDoctorBeforeUpdate() {
        when(doctorCommandMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> doctorCommandService.update(99L, saveRequest("Li Si", 2, 5L, "D-002", 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(departmentCommandMapper, never()).selectById(any());
        verify(doctorCommandMapper, never()).updateById(any(Doctor.class));
    }

    @Test
    void rejectsInvalidStatusWithoutAccessingDatabase() {
        assertThatThrownBy(() -> doctorCommandService.updateStatus(1L, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(doctorCommandMapper, never()).selectById(any());
    }

    @Test
    void updatesStatusForExistingDoctor() {
        when(doctorCommandMapper.selectById(8L)).thenReturn(doctor(8L));

        doctorCommandService.updateStatus(8L, 0);

        verify(doctorCommandMapper).update(isNull(), any());
    }

    private DoctorSaveDTO saveRequest(
            String name,
            Integer gender,
            Long departmentId,
            String doctorCode,
            Integer sortOrder
    ) {
        DoctorSaveDTO request = new DoctorSaveDTO();
        request.setName(name);
        request.setGender(gender);
        request.setDepartmentId(departmentId);
        request.setDoctorCode(doctorCode);
        request.setSortOrder(sortOrder);
        request.setTitle("  Attending physician  ");
        request.setIntroduction("  Internal medicine  ");
        request.setAvatarUrl("  https://example.com/avatar.png  ");
        return request;
    }

    private Department enabledDepartment(Long id) {
        Department department = new Department();
        department.setId(id);
        department.setStatus(1);
        return department;
    }

    private Department disabledDepartment(Long id) {
        Department department = enabledDepartment(id);
        department.setStatus(0);
        return department;
    }

    private Doctor doctor(Long id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        return doctor;
    }
}
