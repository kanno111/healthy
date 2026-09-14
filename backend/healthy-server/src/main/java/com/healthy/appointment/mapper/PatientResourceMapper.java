package com.healthy.appointment.mapper;

import com.healthy.appointment.vo.PatientScheduleSlotVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PatientResourceMapper {
    List<Long> listDoctorIdsWithAvailableSlots(
            @Param("doctorIds") List<Long> doctorIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );

    List<PatientScheduleSlotVO> listScheduleSlots(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
    );
}
