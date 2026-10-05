package com.healthy.appointment.domain.doctor;

import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.DoctorMapper;
import com.healthy.appointment.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LocalDoctorDirectory implements DoctorDirectory {
    private final DoctorMapper doctorMapper;

    @Override
    public DoctorSummary requireDoctor(Long doctorId) {
        DoctorVO doctor = doctorMapper.findById(doctorId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toSummary(doctor);
    }

    @Override
    public DoctorSummary requireEnabledDoctorByUserId(Long userId) {
        Doctor doctor = doctorMapper.findEnabledByUserId(userId);
        if (doctor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        DoctorVO doctorView = doctorMapper.findById(doctor.getId());
        DoctorSummary summary = doctorView == null ? new DoctorSummary() : toSummary(doctorView);
        summary.setId(doctor.getId());
        summary.setUserId(doctor.getUserId());
        summary.setStatus(doctor.getStatus());
        if (summary.getName() == null) {
            summary.setName(doctor.getName());
            summary.setDepartmentId(doctor.getDepartmentId());
        }
        return summary;
    }

    @Override
    public Map<Long, DoctorSummary> findDoctorsByIds(Collection<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return Map.of();
        }
        List<DoctorVO> doctors = doctorMapper.listByIds(doctorIds.stream().distinct().toList());
        return doctors.stream().map(this::toSummary).collect(Collectors.toMap(
                DoctorSummary::getId,
                summary -> summary,
                (first, ignored) -> first,
                LinkedHashMap::new));
    }

    @Override
    public Set<Long> findDoctorIdsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return Set.of();
        }
        return Set.copyOf(doctorMapper.findIdsByDepartment(departmentId));
    }

    private DoctorSummary toSummary(DoctorVO doctor) {
        DoctorSummary summary = new DoctorSummary();
        summary.setId(doctor.getId());
        summary.setName(doctor.getName());
        summary.setStatus(doctor.getStatus());
        summary.setDepartmentId(doctor.getDepartmentId());
        summary.setDepartmentName(doctor.getDepartmentName());
        return summary;
    }
}
