package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueItemVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistVO;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentWaitlistMapper extends BaseMapper<AppointmentWaitlist> {

    AppointmentWaitlist selectFirstWaitingForUpdate(@Param("scheduleSlotId") Long scheduleSlotId);

    List<AppointmentWaitlist> selectExpiredWaiting(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    int expireWaitingIfSlotEnded(
            @Param("waitlistId") Long waitlistId,
            @Param("now") LocalDateTime now);

    List<PatientAppointmentWaitlistVO> listByPatientId(@Param("patientId") Long patientId);

    PatientAppointmentWaitlistVO findByIdAndPatientId(@Param("id") Long id, @Param("patientId") Long patientId);

    IPage<AdminAppointmentWaitlistVO> pageForAdmin(
            @Param("page") IPage<AdminAppointmentWaitlistVO> page,
            @Param("status") String status,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("departmentId") Long departmentId,
            @Param("doctorId") Long doctorId,
            @Param("patientKeyword") String patientKeyword);

    AdminAppointmentWaitlistQueueVO findQueueSummary(@Param("scheduleSlotId") Long scheduleSlotId);

    List<AdminAppointmentWaitlistQueueItemVO> listOfferedCandidates(@Param("scheduleSlotId") Long scheduleSlotId);

    List<AdminAppointmentWaitlistQueueItemVO> listWaitingCandidates(@Param("scheduleSlotId") Long scheduleSlotId);
}
