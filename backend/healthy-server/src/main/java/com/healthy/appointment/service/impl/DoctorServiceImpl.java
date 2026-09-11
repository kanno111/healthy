package com.healthy.appointment.service.impl;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.service.DepartmentService;
import com.healthy.appointment.service.DoctorService;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.appointment.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final DoctorMapper doctorMapper;
    private final DepartmentService departmentService;

    @Override
    public List<DoctorVO> list(String name, Long departmentId, Integer status) {
        validateStatusIfPresent(status);
        return doctorMapper.list(blankToNull(name), departmentId, status);
    }

    @Override
    public DoctorVO getById(Long id) {
        DoctorVO doctor = doctorMapper.findById(id);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return doctor;
    }

    @Override
    public Long create(DoctorSaveDTO doctorSaveDTO) {
        Doctor doctor = toDoctor(doctorSaveDTO);
        validateEnabledDepartment(doctor.getDepartmentId());
        if (doctorMapper.existsByDoctorCode(doctor.getDoctorCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        doctorMapper.insert(doctor);
        return doctor.getId();
    }

    @Override
    public void update(Long id, DoctorSaveDTO doctorSaveDTO) {
        getById(id);
        Doctor doctor = toDoctor(doctorSaveDTO);
        doctor.setId(id);
        validateEnabledDepartment(doctor.getDepartmentId());
        if (doctorMapper.existsByDoctorCode(doctor.getDoctorCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        doctorMapper.update(doctor);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        getById(id);
        doctorMapper.updateStatus(id, status);
    }

    private void validateEnabledDepartment(Long departmentId) {
        DepartmentVO department = departmentService.getById(departmentId);
        if (department.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private Doctor toDoctor(DoctorSaveDTO source) {
        Doctor doctor = new Doctor();
        doctor.setName(source.getName().trim());
        doctor.setGender(source.getGender());
        doctor.setDepartmentId(source.getDepartmentId());
        doctor.setDoctorCode(source.getDoctorCode().trim());
        doctor.setTitle(blankToNull(source.getTitle()));
        doctor.setIntroduction(blankToNull(source.getIntroduction()));
        doctor.setAvatarUrl(blankToNull(source.getAvatarUrl()));
        doctor.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        return doctor;
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
