package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AppointmentWaitlistMapper extends BaseMapper<AppointmentWaitlist> {

    AppointmentWaitlist selectFirstWaitingForUpdate(@Param("scheduleSlotId") Long scheduleSlotId);

    List<PatientAppointmentWaitlistVO> listByPatientId(@Param("patientId") Long patientId);

    PatientAppointmentWaitlistVO findByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);
}
