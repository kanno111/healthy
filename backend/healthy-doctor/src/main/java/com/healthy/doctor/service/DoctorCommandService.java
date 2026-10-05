package com.healthy.doctor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.doctor.mapper.DepartmentCommandMapper;
import com.healthy.doctor.mapper.DoctorCommandMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DoctorCommandService {
    private final DoctorCommandMapper doctorCommandMapper;
    private final DepartmentCommandMapper departmentCommandMapper;

    public DoctorCommandService(
            DoctorCommandMapper doctorCommandMapper,
            DepartmentCommandMapper departmentCommandMapper
    ) {
        this.doctorCommandMapper = doctorCommandMapper;
        this.departmentCommandMapper = departmentCommandMapper;
    }

    public Long create(DoctorSaveDTO request) {
        Doctor doctor = toDoctor(request);
        requireEnabledDepartment(doctor.getDepartmentId());
        ensureDoctorCodeAvailable(doctor.getDoctorCode(), null);

        doctor.setStatus(1);
        doctorCommandMapper.insert(doctor);
        return doctor.getId();
    }

    public void update(Long id, DoctorSaveDTO request) {
        requireDoctor(id);

        Doctor doctor = toDoctor(request);
        doctor.setId(id);
        requireEnabledDepartment(doctor.getDepartmentId());
        ensureDoctorCodeAvailable(doctor.getDoctorCode(), id);

        doctorCommandMapper.updateById(doctor);
    }

    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        requireDoctor(id);

        doctorCommandMapper.update(null, new LambdaUpdateWrapper<>(Doctor.class)
                .eq(Doctor::getId, id)
                .set(Doctor::getStatus, status));
    }

    private Doctor requireDoctor(Long id) {
        Doctor doctor = doctorCommandMapper.selectById(id);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return doctor;
    }

    private void requireEnabledDepartment(Long departmentId) {
        Department department = departmentCommandMapper.selectById(departmentId);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(department.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void ensureDoctorCodeAvailable(String doctorCode, Long excludedDoctorId) {
        LambdaQueryWrapper<Doctor> query = new LambdaQueryWrapper<>(Doctor.class)
                .eq(Doctor::getDoctorCode, doctorCode);
        if (excludedDoctorId != null) {
            query.ne(Doctor::getId, excludedDoctorId);
        }
        if (doctorCommandMapper.exists(query)) {
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

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
