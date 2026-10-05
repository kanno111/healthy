package com.healthy.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.healthy.identity.entity.Patient;
import com.healthy.identity.model.PatientQueryView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PatientMapper extends BaseMapper<Patient> {
    PatientQueryView findEnabledByUserId(@Param("userId") Long userId);

    List<PatientQueryView> listViewsByIds(@Param("patientIds") List<Long> patientIds);

    List<Long> findIdsByKeyword(@Param("keyword") String keyword);
}
