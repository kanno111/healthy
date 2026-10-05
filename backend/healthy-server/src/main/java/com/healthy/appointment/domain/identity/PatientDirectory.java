package com.healthy.appointment.domain.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Booking-facing boundary for patient identity data. */
public interface PatientDirectory {
    Long requireEnabledPatientIdByUserId(Long userId);

    Map<Long, PatientSummary> findByIds(Collection<Long> patientIds);

    Set<Long> findIdsByKeyword(String keyword);
}
