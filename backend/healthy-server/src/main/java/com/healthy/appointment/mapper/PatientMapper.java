package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.healthy.appointment.domain.identity.PatientSummary;
import com.healthy.appointment.entity.Patient;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PatientMapper extends BaseMapper<Patient> {
    Patient findEnabledByUserId(@Param("userId") Long userId);

    List<PatientSummary> listSummariesByIds(@Param("patientIds") List<Long> patientIds);

    List<Long> findIdsByKeyword(@Param("keyword") String keyword);
}
