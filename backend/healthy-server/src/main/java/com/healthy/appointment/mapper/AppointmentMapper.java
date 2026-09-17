package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.vo.PatientAppointmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppointmentMapper {
    int insert(@Param("appointment") Appointment appointment);

    PatientAppointmentVO findByRequestIdAndPatientId(
            @Param("requestId") String requestId,
            @Param("patientId") Long patientId
    );

    PatientAppointmentVO findActiveByPatientIdAndScheduleSlotId(
            @Param("patientId") Long patientId,
            @Param("scheduleSlotId") Long scheduleSlotId
    );

    Appointment findEntityByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    int cancelBooked(@Param("id") Long id, @Param("patientId") Long patientId);

    PatientAppointmentVO findByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    List<PatientAppointmentVO> listByPatientId(@Param("patientId") Long patientId);
}
