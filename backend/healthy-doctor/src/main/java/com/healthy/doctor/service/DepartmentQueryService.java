package com.healthy.doctor.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.appointment.vo.PatientDepartmentVO;
import com.healthy.doctor.mapper.DepartmentQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentQueryService {
    private final DepartmentQueryMapper departmentQueryMapper;

    public DepartmentQueryService(DepartmentQueryMapper departmentQueryMapper) {
        this.departmentQueryMapper = departmentQueryMapper;
    }

    public List<DepartmentVO> list(String name, Integer status) {
        validateStatusIfPresent(status);
        return departmentQueryMapper.list(blankToNull(name), status);
    }

    public DepartmentVO getById(Long id) {
        DepartmentVO department = departmentQueryMapper.findById(id);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return department;
    }

    public List<PatientDepartmentVO> listVisible() {
        return departmentQueryMapper.list(null, 1).stream()
                .map(this::toPatientDepartment)
                .toList();
    }

    private PatientDepartmentVO toPatientDepartment(DepartmentVO source) {
        PatientDepartmentVO target = new PatientDepartmentVO();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        return target;
    }

    private void validateStatusIfPresent(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
