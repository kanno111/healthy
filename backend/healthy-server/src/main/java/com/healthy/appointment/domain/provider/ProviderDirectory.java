package com.healthy.appointment.domain.provider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Booking-facing boundary for doctor and department master data. */
public interface ProviderDirectory {
    DoctorSummary requireDoctor(Long doctorId);

    DoctorSummary requireEnabledDoctorByUserId(Long userId);

    Map<Long, DoctorSummary> findDoctorsByIds(Collection<Long> doctorIds);

    Set<Long> findDoctorIdsByDepartment(Long departmentId);
}
