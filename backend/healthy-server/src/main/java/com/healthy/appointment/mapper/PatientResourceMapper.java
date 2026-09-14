package com.healthy.appointment.mapper;

import com.healthy.appointment.vo.PatientDepartmentVO;
import com.healthy.appointment.vo.PatientDoctorVO;
import com.healthy.appointment.vo.PatientScheduleSlotVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface PatientResourceMapper {
    List<PatientDepartmentVO> listDepartments();

    List<PatientDoctorVO> listDoctors(@Param("departmentId") Long departmentId, @Param("keyword") String keyword);

    PatientDoctorVO findDoctorById(@Param("id") Long id);

    List<PatientScheduleSlotVO> listScheduleSlots(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
