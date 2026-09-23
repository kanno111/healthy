package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.DepartmentService;
import com.healthy.appointment.service.DoctorService;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.appointment.vo.DoctorVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.healthy.appointment.constant.Constant.MAX_PATIENT_SCHEDULE_QUERY_DAYS;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {
    private final DoctorMapper doctorMapper;
    private final DepartmentService departmentService;

    public DoctorServiceImpl(DoctorMapper doctorMapper, DepartmentService departmentService) {
        this.doctorMapper = doctorMapper;
        this.departmentService = departmentService;
        this.baseMapper = doctorMapper;
    }

    @Override
    public List<DoctorVO> list(String name, Long departmentId, Integer status) {
        validateStatusIfPresent(status);
        return doctorMapper.list(blankToNull(name), departmentId, status);
    }

    @Override
    public PageResult<DoctorVO> page(String name, Long departmentId, Integer status, int page, int pageSize) {
        validateStatusIfPresent(status);
        return pageDoctors(blankToNull(name), null, departmentId, status, null,
                false, null, null, null, page, pageSize);
    }

    @Override
    public PageResult<DoctorVO> pageVisible(Long departmentId, String keyword, int page, int pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = now.toLocalDate();
        return pageDoctors(null, blankToNull(keyword), departmentId, 1, 1,
                true, currentDate, currentDate.plusDays(MAX_PATIENT_SCHEDULE_QUERY_DAYS - 1L),
                now.toLocalTime(), page, pageSize);
    }

    private PageResult<DoctorVO> pageDoctors(
            String name,
            String keyword,
            Long departmentId,
            Integer status,
            Integer departmentStatus,
            boolean availableFirst,
            LocalDate availabilityStartDate,
            LocalDate availabilityEndDate,
            LocalTime currentTime,
            int page,
            int pageSize
    ) {
        validatePage(page, pageSize);
        IPage<DoctorVO> result = doctorMapper.page(
                new Page<>(page, pageSize), name, keyword, departmentId, status, departmentStatus,
                availableFirst, availabilityStartDate, availabilityEndDate, currentTime);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
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
        if (doctorMapper.exists(new LambdaQueryWrapper<Doctor>().eq(Doctor::getDoctorCode, doctor.getDoctorCode()))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        doctor.setStatus(1);
        save(doctor);
        return doctor.getId();
    }

    @Override
    public void update(Long id, DoctorSaveDTO doctorSaveDTO) {
        getById(id);
        Doctor doctor = toDoctor(doctorSaveDTO);
        doctor.setId(id);
        validateEnabledDepartment(doctor.getDepartmentId());
        if (doctorMapper.exists(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getDoctorCode, doctor.getDoctorCode()).ne(Doctor::getId, id))) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        updateById(doctor);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        getById(id);
        doctorMapper.update(null, new LambdaUpdateWrapper<Doctor>()
                .eq(Doctor::getId, id).set(Doctor::getStatus, status));
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

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
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
