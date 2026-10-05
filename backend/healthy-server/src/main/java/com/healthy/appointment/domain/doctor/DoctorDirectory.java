package com.healthy.appointment.domain.doctor;

import com.healthy.appointment.result.PageResult;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Booking-facing boundary for doctor and department master data. */
public interface DoctorDirectory {
    DoctorSummary requireDoctor(Long doctorId);

    DoctorSummary requireVisibleDoctor(Long doctorId);

    DoctorSummary requireEnabledDoctorByUserId(Long userId);

    PageResult<DoctorSummary> pageVisibleDoctors(
            Long departmentId, String keyword, int page, int pageSize);

    Map<Long, DoctorSummary> findDoctorsByIds(Collection<Long> doctorIds);

    Set<Long> findDoctorIdsByDepartment(Long departmentId);
}
