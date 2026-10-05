package com.healthy.doctor.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.result.PageResult;
import com.healthy.doctor.mapper.DoctorQueryMapper;
import com.healthy.doctor.model.DoctorQueryView;
import com.healthy.doctor.vo.DoctorVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class DoctorQueryService {
    private static final int MAX_BATCH_SIZE = 100;

    private final DoctorQueryMapper doctorQueryMapper;

    public DoctorQueryService(DoctorQueryMapper doctorQueryMapper) {
        this.doctorQueryMapper = doctorQueryMapper;
    }

    public PageResult<DoctorVO> page(
            String name, Long departmentId, Integer status, int page, int pageSize) {
        validateStatusIfPresent(status);
        PageResult<DoctorQueryView> result = pageDoctors(
                blankToNull(name), null, departmentId, status, null, page, pageSize);
        return PageResult.of(
                result.records().stream().map(this::toDoctorVO).toList(),
                result.total(),
                result.page(),
                result.pageSize()
        );
    }

    public PageResult<DoctorQueryView> pageVisible(
            Long departmentId, String keyword, int page, int pageSize) {
        return pageDoctors(null, blankToNull(keyword), departmentId, 1, 1, page, pageSize);
    }

    public DoctorQueryView getById(Long id) {
        DoctorQueryView doctor = doctorQueryMapper.findById(id);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return doctor;
    }

    public DoctorVO getAdminById(Long id) {
        return toDoctorVO(getById(id));
    }

    public DoctorQueryView getEnabledByUserId(Long userId) {
        DoctorQueryView doctor = doctorQueryMapper.findEnabledByUserId(userId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return doctor;
    }

    public List<DoctorQueryView> listByIds(List<Long> doctorIds) {
        if (doctorIds == null || doctorIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        List<Long> normalizedIds = doctorIds.stream().distinct().toList();
        if (normalizedIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalizedIds.isEmpty() ? List.of() : doctorQueryMapper.listByIds(normalizedIds);
    }

    public List<Long> findIdsByDepartment(Long departmentId) {
        return doctorQueryMapper.findIdsByDepartment(departmentId);
    }

    private PageResult<DoctorQueryView> pageDoctors(
            String name,
            String keyword,
            Long departmentId,
            Integer status,
            Integer departmentStatus,
            int page,
            int pageSize
    ) {
        validatePage(page, pageSize);
        IPage<DoctorQueryView> result = doctorQueryMapper.page(
                new Page<>(page, pageSize), name, keyword, departmentId, status, departmentStatus);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    private void validateStatusIfPresent(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DoctorVO toDoctorVO(DoctorQueryView source) {
        DoctorVO target = new DoctorVO();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setGender(source.getGender());
        target.setDepartmentId(source.getDepartmentId());
        target.setDepartmentName(source.getDepartmentName());
        target.setDoctorCode(source.getDoctorCode());
        target.setTitle(source.getTitle());
        target.setIntroduction(source.getIntroduction());
        target.setAvatarUrl(source.getAvatarUrl());
        target.setSortOrder(source.getSortOrder());
        target.setStatus(source.getStatus());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
