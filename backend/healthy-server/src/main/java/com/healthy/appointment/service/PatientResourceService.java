package com.healthy.appointment.service;

import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.PatientDepartmentVO;
import com.healthy.appointment.vo.PatientDoctorVO;
import com.healthy.appointment.vo.PatientScheduleSlotVO;

import java.time.LocalDate;
import java.util.List;

public interface PatientResourceService {
    List<PatientDepartmentVO> listDepartments();

    PageResult<PatientDoctorVO> pageDoctors(Long departmentId, String keyword, int page, int pageSize);

    PatientDoctorVO getDoctorById(Long id);

    List<PatientScheduleSlotVO> listScheduleSlots(Long doctorId, LocalDate startDate, LocalDate endDate);
}
