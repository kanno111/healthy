package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.vo.ScheduleSlotVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDate;

public interface DoctorScheduleSlotMapper {
    List<DoctorScheduleSlot> findOverlappingSlots(
            @Param("doctorId") Long doctorId,
            @Param("slots") List<DoctorScheduleSlot> slots
    );

    int batchInsert(@Param("slots") List<DoctorScheduleSlot> slots);

    List<ScheduleSlotVO> list(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    DoctorScheduleSlot findById(@Param("id") Long id);

    int decreaseRemainingCapacity(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("version") Integer version);

    int updateCapacity(@Param("id") Long id, @Param("capacity") Integer capacity, @Param("version") Integer version);
}
