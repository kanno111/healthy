package com.healthy.doctor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.doctor.mapper.DepartmentCommandMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentCommandService {
    private final DepartmentCommandMapper departmentCommandMapper;

    public DepartmentCommandService(DepartmentCommandMapper departmentCommandMapper) {
        this.departmentCommandMapper = departmentCommandMapper;
    }

    public Long create(DepartmentSaveDTO request) {
        Department department = toDepartment(request);
        if (departmentCommandMapper.exists(new LambdaQueryWrapper<>(Department.class)
                .eq(Department::getName, department.getName()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        department.setStatus(1);
        departmentCommandMapper.insert(department);
        return department.getId();
    }

    public void update(Long id, DepartmentSaveDTO request) {
        requireDepartment(id);

        Department department = toDepartment(request);
        department.setId(id);
        if (departmentCommandMapper.exists(new LambdaQueryWrapper<>(Department.class)
                .eq(Department::getName, department.getName())
                .ne(Department::getId, id))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        departmentCommandMapper.updateById(department);
    }

    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        requireDepartment(id);

        departmentCommandMapper.update(null, new LambdaUpdateWrapper<>(Department.class)
                .eq(Department::getId, id)
                .set(Department::getStatus, status));
    }

    private Department requireDepartment(Long id) {
        Department department = departmentCommandMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return department;
    }

    private Department toDepartment(DepartmentSaveDTO source) {
        Department department = new Department();
        department.setName(source.getName().trim());
        department.setDescription(blankToNull(source.getDescription()));
        department.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        return department;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
