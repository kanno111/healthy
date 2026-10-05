package com.healthy.appointment.domain.identity;

import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.PatientMapper;
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
public class LocalPatientDirectory implements PatientDirectory {
    private final PatientMapper patientMapper;

    @Override
    public Long requireEnabledPatientIdByUserId(Long userId) {
        Patient patient = patientMapper.findEnabledByUserId(userId);
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return patient.getId();
    }

    @Override
    public Map<Long, PatientSummary> findByIds(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Map.of();
        }
        List<PatientSummary> summaries = patientMapper.listSummariesByIds(patientIds.stream().distinct().toList());
        return summaries.stream().collect(Collectors.toMap(
                PatientSummary::getId,
                summary -> summary,
                (first, ignored) -> first,
                LinkedHashMap::new));
    }

    @Override
    public Set<Long> findIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Set.of();
        }
        return Set.copyOf(patientMapper.findIdsByKeyword(keyword.trim()));
    }
}
