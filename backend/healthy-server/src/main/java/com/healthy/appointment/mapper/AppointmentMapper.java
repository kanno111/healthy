package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.healthy.appointment.entity.Appointment;
import com.healthy.appointment.vo.AdminAppointmentVO;
import com.healthy.appointment.vo.PatientAppointmentVO;
import com.healthy.appointment.vo.DoctorAppointmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

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

    Appointment findById(@Param("id") Long id);

    int completeBooked(@Param("id") Long id);

    IPage<DoctorAppointmentVO> pageForDoctor(
            @Param("page") IPage<DoctorAppointmentVO> page,
            @Param("doctorId") Long doctorId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("status") String status
    );

    int completeBookedByDoctor(
            @Param("id") Long id,
            @Param("doctorId") Long doctorId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime);

    List<AdminAppointmentVO> listForAdmin();

    PatientAppointmentVO findByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    List<PatientAppointmentVO> listByPatientId(@Param("patientId") Long patientId);
}
