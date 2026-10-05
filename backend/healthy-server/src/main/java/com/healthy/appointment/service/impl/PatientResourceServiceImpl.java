package com.healthy.appointment.service.impl;

import com.healthy.appointment.domain.doctor.DoctorDirectory;
import com.healthy.appointment.domain.doctor.DoctorSummary;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.PatientResourceMapper;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.PatientResourceService;
import com.healthy.appointment.vo.PatientDoctorVO;
import com.healthy.appointment.vo.PatientScheduleSlotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.healthy.appointment.constant.Constant.MAX_PATIENT_SCHEDULE_QUERY_DAYS;

@Service
@RequiredArgsConstructor
public class PatientResourceServiceImpl implements PatientResourceService {
    private final DoctorDirectory doctorDirectory;
    private final PatientResourceMapper patientResourceMapper;

    @Override
    public PageResult<PatientDoctorVO> pageDoctors(Long departmentId, String keyword, int page, int pageSize) {
        PageResult<DoctorSummary> doctorPage =
                doctorDirectory.pageVisibleDoctors(departmentId, keyword, page, pageSize);
        List<DoctorSummary> doctors = doctorPage.records();
        Set<Long> availableDoctorIds = findAvailableDoctorIds(
                doctors.stream().map(DoctorSummary::getId).toList());
        List<PatientDoctorVO> records = doctors.stream()
                .map(doctor -> toPatientDoctorVO(doctor, availableDoctorIds.contains(doctor.getId())))
                .toList();
        return PageResult.of(records, doctorPage.total(), doctorPage.page(), doctorPage.pageSize());
    }

    @Override
    public PatientDoctorVO getDoctorById(Long id) {
        DoctorSummary doctor = doctorDirectory.requireVisibleDoctor(id);
        boolean hasAvailableSlots = findAvailableDoctorIds(List.of(id)).contains(id);
        return toPatientDoctorVO(doctor, hasAvailableSlots);
    }

    @Override
    public List<PatientScheduleSlotVO> listScheduleSlots(Long doctorId, LocalDate startDate, LocalDate endDate) {
        doctorDirectory.requireVisibleDoctor(doctorId);
        validateDateRange(startDate, endDate);
        LocalDate currentDate = LocalDate.now();
        return patientResourceMapper.listScheduleSlots(doctorId, startDate, endDate, currentDate, LocalTime.now());
    }

    private Set<Long> findAvailableDoctorIds(List<Long> doctorIds) {
        if (doctorIds.isEmpty()) {
            return Set.of();
        }
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(MAX_PATIENT_SCHEDULE_QUERY_DAYS - 1L);
        return new HashSet<>(patientResourceMapper.listDoctorIdsWithAvailableSlots(
                doctorIds, startDate, endDate, startDate, LocalTime.now()
        ));
    }

    private PatientDoctorVO toPatientDoctorVO(DoctorSummary source, boolean hasAvailableSlots) {
        PatientDoctorVO target = new PatientDoctorVO();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setGender(source.getGender());
        target.setDepartmentId(source.getDepartmentId());
        target.setDepartmentName(source.getDepartmentName());
        target.setTitle(source.getTitle());
        target.setIntroduction(source.getIntroduction());
        target.setAvatarUrl(source.getAvatarUrl());
        target.setHasAvailableSlots(hasAvailableSlots);
        return target;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate == null || endDate == null
                || startDate.isBefore(today)
                || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_PATIENT_SCHEDULE_QUERY_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

}
