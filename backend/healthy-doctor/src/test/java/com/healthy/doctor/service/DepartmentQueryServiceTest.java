package com.healthy.doctor.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.doctor.mapper.DepartmentQueryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentQueryServiceTest {
    @Mock
    private DepartmentQueryMapper departmentQueryMapper;

    @InjectMocks
    private DepartmentQueryService departmentQueryService;

    @Test
    void normalizesNameBeforeListingDepartments() {
        DepartmentVO department = department(1L, "Cardiology");
        when(departmentQueryMapper.list("Cardiology", 1)).thenReturn(List.of(department));

        List<DepartmentVO> result = departmentQueryService.list("  Cardiology  ", 1);

        assertThat(result).containsExactly(department);
        verify(departmentQueryMapper).list("Cardiology", 1);
    }

    @Test
    void convertsBlankNameToNull() {
        when(departmentQueryMapper.list(null, null)).thenReturn(List.of());

        departmentQueryService.list("   ", null);

        verify(departmentQueryMapper).list(null, null);
    }

    @Test
    void rejectsUnsupportedStatus() {
        assertThatThrownBy(() -> departmentQueryService.list(null, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void returnsDepartmentById() {
        DepartmentVO department = department(1L, "Cardiology");
        when(departmentQueryMapper.findById(1L)).thenReturn(department);

        assertThat(departmentQueryService.getById(1L)).isSameAs(department);
    }

    @Test
    void reportsMissingDepartment() {
        when(departmentQueryMapper.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> departmentQueryService.getById(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void listsOnlyEnabledDepartmentsForPatients() {
        DepartmentVO department = department(1L, "Cardiology");
        department.setDescription("Heart care");
        department.setStatus(1);
        department.setDoctorCount(6L);
        when(departmentQueryMapper.list(null, 1)).thenReturn(List.of(department));

        var result = departmentQueryService.listVisible();

        assertThat(result).singleElement().satisfies(patientDepartment -> {
            assertThat(patientDepartment.getId()).isEqualTo(1L);
            assertThat(patientDepartment.getName()).isEqualTo("Cardiology");
            assertThat(patientDepartment.getDescription()).isEqualTo("Heart care");
        });
        verify(departmentQueryMapper).list(null, 1);
    }

    private DepartmentVO department(Long id, String name) {
        DepartmentVO department = new DepartmentVO();
        department.setId(id);
        department.setName(name);
        return department;
    }
}
