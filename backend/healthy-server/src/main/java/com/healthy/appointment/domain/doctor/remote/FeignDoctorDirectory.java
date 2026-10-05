package com.healthy.appointment.domain.doctor.remote;

import com.healthy.appointment.domain.doctor.DoctorDirectory;
import com.healthy.appointment.domain.doctor.DoctorSummary;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
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
public class FeignDoctorDirectory implements DoctorDirectory {
    private static final int REMOTE_BATCH_SIZE = 100;

    private final DoctorServiceClient doctorServiceClient;

    @Override
    public DoctorSummary requireDoctor(Long doctorId) {
        DoctorRemoteView doctor = invoke(
                () -> doctorServiceClient.getDoctor(doctorId), ErrorCode.NOT_FOUND);
        return toSummary(doctor);
    }

    @Override
    public DoctorSummary requireVisibleDoctor(Long doctorId) {
        DoctorSummary doctor = requireDoctor(doctorId);
        if (!Integer.valueOf(1).equals(doctor.getStatus())
                || !Integer.valueOf(1).equals(doctor.getDepartmentStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return doctor;
    }

    @Override
    public DoctorSummary requireEnabledDoctorByUserId(Long userId) {
        DoctorRemoteView doctor = invoke(
                () -> doctorServiceClient.getEnabledDoctorByUserId(userId), ErrorCode.FORBIDDEN);
        return toSummary(doctor);
    }

    @Override
    public PageResult<DoctorSummary> pageVisibleDoctors(
            Long departmentId, String keyword, int page, int pageSize
    ) {
        PageResult<DoctorRemoteView> result = invoke(
                () -> doctorServiceClient.pageVisibleDoctors(
                        departmentId, keyword, page, pageSize),
                ErrorCode.NOT_FOUND);
        List<DoctorSummary> records = result.records().stream()
                .map(this::toSummary)
                .toList();
        return PageResult.of(records, result.total(), result.page(), result.pageSize());
    }

    @Override
    public Map<Long, DoctorSummary> findDoctorsByIds(Collection<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            return Map.of();
        }
        List<Long> normalizedIds = doctorIds.stream().distinct().toList();
        List<DoctorRemoteView> remoteDoctors = new ArrayList<>();
        for (int start = 0; start < normalizedIds.size(); start += REMOTE_BATCH_SIZE) {
            int end = Math.min(start + REMOTE_BATCH_SIZE, normalizedIds.size());
            List<Long> batch = normalizedIds.subList(start, end);
            remoteDoctors.addAll(invoke(
                    () -> doctorServiceClient.listDoctorsByIds(batch), ErrorCode.INTERNAL_ERROR));
        }

        Map<Long, DoctorSummary> summaries = new LinkedHashMap<>();
        for (DoctorRemoteView doctor : remoteDoctors) {
            DoctorSummary summary = toSummary(doctor);
            summaries.putIfAbsent(summary.getId(), summary);
        }
        return summaries;
    }

    @Override
    public Set<Long> findDoctorIdsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return Set.of();
        }
        List<Long> doctorIds = invoke(
                () -> doctorServiceClient.listDoctorIdsByDepartment(departmentId), ErrorCode.INTERNAL_ERROR);
        return new LinkedHashSet<>(doctorIds);
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

    private DoctorSummary toSummary(DoctorRemoteView doctor) {
        DoctorSummary summary = new DoctorSummary();
        summary.setId(doctor.getId());
        summary.setUserId(doctor.getUserId());
        summary.setName(doctor.getName());
        summary.setGender(doctor.getGender());
        summary.setStatus(doctor.getStatus());
        summary.setDepartmentId(doctor.getDepartmentId());
        summary.setDepartmentName(doctor.getDepartmentName());
        summary.setDepartmentStatus(doctor.getDepartmentStatus());
        summary.setTitle(doctor.getTitle());
        summary.setIntroduction(doctor.getIntroduction());
        summary.setAvatarUrl(doctor.getAvatarUrl());
        return summary;
    }
}
