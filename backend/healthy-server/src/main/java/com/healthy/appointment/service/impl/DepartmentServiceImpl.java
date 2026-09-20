package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DepartmentMapper;
import com.healthy.appointment.service.DepartmentService;
import com.healthy.appointment.vo.DepartmentVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {
    private final DepartmentMapper departmentMapper;

    public DepartmentServiceImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
        this.baseMapper = departmentMapper;
    }

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
        if (departmentMapper.exists(new LambdaQueryWrapper<Department>().eq(Department::getName, department.getName()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        department.setStatus(1);
        save(department);
        return department.getId();
    }

    @Override
    public void update(Long id, DepartmentSaveDTO departmentSaveDTO) {
        getById(id);
        Department department = toDepartment(departmentSaveDTO);
        department.setId(id);
        if (departmentMapper.exists(new LambdaQueryWrapper<Department>()
                .eq(Department::getName, department.getName()).ne(Department::getId, id))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        updateById(department);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        getById(id);
        departmentMapper.update(null, new LambdaUpdateWrapper<Department>()
                .eq(Department::getId, id).set(Department::getStatus, status));
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
