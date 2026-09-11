package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.Patient;
import org.apache.ibatis.annotations.Param;

public interface PatientMapper {
    int insert(@Param("patient") Patient patient);
}
