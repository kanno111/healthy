package com.healthy.identity.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.identity.mapper.PatientMapper;
import com.healthy.identity.model.PatientQueryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientQueryService {
    private static final int MAX_BATCH_SIZE = 100;

    private final PatientMapper patientMapper;

    public PatientQueryView requireEnabledByUserId(Long userId) {
        PatientQueryView patient = patientMapper.findEnabledByUserId(userId);
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return patient;
    }

    public List<PatientQueryView> findByIds(List<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedIds = patientIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return normalizedIds.isEmpty() ? List.of() : patientMapper.listViewsByIds(normalizedIds);
    }

    public List<Long> findIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return patientMapper.findIdsByKeyword(keyword.trim());
    }
}
