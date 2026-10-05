package com.healthy.appointment.domain.doctor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Booking-facing boundary for doctor and department master data. */
public interface DoctorDirectory {
    DoctorSummary requireDoctor(Long doctorId);

    DoctorSummary requireEnabledDoctorByUserId(Long userId);

    Map<Long, DoctorSummary> findDoctorsByIds(Collection<Long> doctorIds);

    Set<Long> findDoctorIdsByDepartment(Long departmentId);
}
