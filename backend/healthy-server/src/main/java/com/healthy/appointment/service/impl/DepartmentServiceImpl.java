package com.healthy.appointment.service.impl;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DepartmentMapper;
import com.healthy.appointment.service.DepartmentService;
import com.healthy.appointment.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentMapper departmentMapper;

    @Override
    public List<DepartmentVO> list(String name, Integer status) {
        validateStatusIfPresent(status);
        return departmentMapper.list(blankToNull(name), status);
    }

    @Override
    public DepartmentVO getById(Long id) {
        DepartmentVO department = departmentMapper.findById(id);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return department;
    }

    @Override
    public Long create(DepartmentSaveDTO departmentSaveDTO) {
        Department department = toDepartment(departmentSaveDTO);
        if (departmentMapper.existsByName(department.getName(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        departmentMapper.insert(department);
        return department.getId();
    }

    @Override
    public void update(Long id, DepartmentSaveDTO departmentSaveDTO) {
        getById(id);
        Department department = toDepartment(departmentSaveDTO);
        department.setId(id);
        if (departmentMapper.existsByName(department.getName(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        departmentMapper.update(department);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        getById(id);
        departmentMapper.updateStatus(id, status);
    }

    private Department toDepartment(DepartmentSaveDTO source) {
        Department department = new Department();
        department.setName(source.getName().trim());
        department.setDescription(blankToNull(source.getDescription()));
        department.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        return department;
    }

    private void validateStatusIfPresent(Integer status) {
        if (status != null) {
            validateStatus(status);
        }
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
