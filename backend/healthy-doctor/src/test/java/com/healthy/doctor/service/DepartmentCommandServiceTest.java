package com.healthy.doctor.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.doctor.dto.DepartmentSaveDTO;
import com.healthy.doctor.entity.Department;
import com.healthy.doctor.mapper.DepartmentCommandMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
class DepartmentCommandServiceTest {
    @BeforeAll
    static void initializeDepartmentTableInfo() {
        if (TableInfoHelper.getTableInfo(Department.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "unit-test-mapper"),
                    Department.class
            );
        }
    }

    @Mock
    private DepartmentCommandMapper departmentCommandMapper;

    @InjectMocks
    private DepartmentCommandService departmentCommandService;

    @Test
    void createsEnabledDepartmentWithNormalizedFields() {
        when(departmentCommandMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            invocation.getArgument(0, Department.class).setId(10L);
            return 1;
        }).when(departmentCommandMapper).insert(any(Department.class));

        Long id = departmentCommandService.create(saveRequest("  Cardiology  ", "  Heart care  ", null));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentCommandMapper).insert(captor.capture());
        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue())
                .extracting(Department::getName, Department::getDescription, Department::getSortOrder, Department::getStatus)
                .containsExactly("Cardiology", "Heart care", 0, 1);
    }

    @Test
    void rejectsDuplicateDepartmentNameOnCreate() {
        when(departmentCommandMapper.exists(any())).thenReturn(true);

        assertThatThrownBy(() -> departmentCommandService.create(saveRequest("Cardiology", null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        verify(departmentCommandMapper, never()).insert(any(Department.class));
    }

    @Test
    void updatesExistingDepartment() {
        when(departmentCommandMapper.selectById(8L)).thenReturn(department(8L, "Cardiology"));
        when(departmentCommandMapper.exists(any())).thenReturn(false);

        departmentCommandService.update(8L, saveRequest("  Neurology  ", "   ", 3));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentCommandMapper).updateById(captor.capture());
        assertThat(captor.getValue())
                .extracting(Department::getId, Department::getName, Department::getDescription, Department::getSortOrder)
                .containsExactly(8L, "Neurology", null, 3);
    }

    @Test
    void reportsMissingDepartmentBeforeUpdate() {
        when(departmentCommandMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> departmentCommandService.update(99L, saveRequest("Neurology", null, 0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(departmentCommandMapper, never()).updateById(any(Department.class));
    }

    @Test
    void rejectsInvalidStatusWithoutAccessingDatabase() {
        assertThatThrownBy(() -> departmentCommandService.updateStatus(1L, 2))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(departmentCommandMapper, never()).selectById(any());
    }

    @Test
    void updatesStatusForExistingDepartment() {
        when(departmentCommandMapper.selectById(8L)).thenReturn(department(8L, "Cardiology"));

        departmentCommandService.updateStatus(8L, 0);

        verify(departmentCommandMapper).update(isNull(), any());
    }

    private DepartmentSaveDTO saveRequest(String name, String description, Integer sortOrder) {
        DepartmentSaveDTO request = new DepartmentSaveDTO();
        request.setName(name);
        request.setDescription(description);
        request.setSortOrder(sortOrder);
        return request;
    }

    private Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        return department;
    }
}
