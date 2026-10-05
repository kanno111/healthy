package com.healthy.appointment.domain.identity.remote;

import com.healthy.appointment.domain.identity.PatientDirectory;
import com.healthy.appointment.domain.identity.PatientSummary;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.result.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class FeignPatientDirectory implements PatientDirectory {
    private static final int REMOTE_BATCH_SIZE = 100;

    private final IdentityServiceClient identityServiceClient;

    @Override
    public Long requireEnabledPatientIdByUserId(Long userId) {
        PatientRemoteView patient = invoke(
                () -> identityServiceClient.getEnabledPatientByUserId(userId), ErrorCode.NOT_FOUND);
        return patient.getId();
    }

    @Override
    public Map<Long, PatientSummary> findByIds(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Map.of();
        }

        List<Long> normalizedIds = patientIds.stream().distinct().toList();
        List<PatientRemoteView> remotePatients = new ArrayList<>();
        for (int start = 0; start < normalizedIds.size(); start += REMOTE_BATCH_SIZE) {
            int end = Math.min(start + REMOTE_BATCH_SIZE, normalizedIds.size());
            List<Long> batch = normalizedIds.subList(start, end);
            remotePatients.addAll(invoke(
                    () -> identityServiceClient.listPatientsByIds(batch), ErrorCode.INTERNAL_ERROR));
        }

        Map<Long, PatientSummary> summaries = new LinkedHashMap<>();
        for (PatientRemoteView patient : remotePatients) {
            PatientSummary summary = toSummary(patient);
            summaries.putIfAbsent(summary.getId(), summary);
        }
        return summaries;
    }

    @Override
    public Set<Long> findIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Set.of();
        }
        List<Long> patientIds = invoke(
                () -> identityServiceClient.findPatientIdsByKeyword(keyword.trim()),
                ErrorCode.INTERNAL_ERROR);
        return new LinkedHashSet<>(patientIds);
    }

    private <T> T invoke(Supplier<ApiResponse<T>> request, ErrorCode notFoundCode) {
        try {
            ApiResponse<T> response = request.get();
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
            return response.data();
        } catch (FeignException exception) {
            throw switch (exception.status()) {
                case 400 -> new BusinessException(ErrorCode.VALIDATION_ERROR);
                case 403 -> new BusinessException(ErrorCode.FORBIDDEN);
                case 404 -> new BusinessException(notFoundCode);
                default -> new BusinessException(ErrorCode.INTERNAL_ERROR);
            };
        }
    }

    private PatientSummary toSummary(PatientRemoteView patient) {
        PatientSummary summary = new PatientSummary();
        summary.setId(patient.getId());
        summary.setUserId(patient.getUserId());
        summary.setName(patient.getName());
        summary.setUsername(patient.getUsername());
        summary.setPhone(patient.getPhone());
        summary.setStatus(patient.getStatus());
        return summary;
    }
}
