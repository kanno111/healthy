package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.vo.PatientAppointmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppointmentMapper {
    int insert(@Param("appointment") Appointment appointment);

    PatientAppointmentVO findByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    List<PatientAppointmentVO> listByPatientId(@Param("patientId") Long patientId);
}
