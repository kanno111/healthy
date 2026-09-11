package com.healthy.appointment.service;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DepartmentMapper;
import com.healthy.appointment.service.impl.DepartmentServiceImpl;
import com.healthy.appointment.vo.DepartmentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {
    @Mock
    private DepartmentMapper departmentMapper;

    @Test
    void createTrimsNameAndUsesDefaultSortOrder() {
        DepartmentService service = new DepartmentServiceImpl(departmentMapper);
        when(departmentMapper.existsByName("cardiology", null)).thenReturn(false);
        doAnswer(invocation -> {
            invocation.getArgument(0, Department.class).setId(10L);
            return 1;
        }).when(departmentMapper).insert(any(Department.class));

        Long id = service.create(saveRequest(" cardiology ", null));

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(captor.capture());
        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue().getName()).isEqualTo("cardiology");
        assertThat(captor.getValue().getSortOrder()).isZero();
    }

    @Test
    void createRejectsDuplicateName() {
        DepartmentService service = new DepartmentServiceImpl(departmentMapper);
        when(departmentMapper.existsByName("cardiology", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(saveRequest("cardiology", 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatusRejectsInvalidValue() {
        DepartmentService service = new DepartmentServiceImpl(departmentMapper);

        assertThatThrownBy(() -> service.updateStatus(1L, 2))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateStatusUpdatesExistingDepartment() {
        DepartmentService service = new DepartmentServiceImpl(departmentMapper);
        when(departmentMapper.findById(1L)).thenReturn(departmentView());

        service.updateStatus(1L, 0);

        verify(departmentMapper).updateStatus(1L, 0);
    }

    private DepartmentSaveDTO saveRequest(String name, Integer sortOrder) {
        DepartmentSaveDTO request = new DepartmentSaveDTO();
        request.setName(name);
        request.setSortOrder(sortOrder);
        return request;
    }

    private DepartmentVO departmentView() {
        DepartmentVO department = new DepartmentVO();
        department.setId(1L);
        department.setName("cardiology");
        department.setSortOrder(0);
        department.setStatus(1);
        department.setDoctorCount(0L);
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        return department;
    }
}
