package com.healthy.appointment.service;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.vo.DoctorVO;

import java.util.List;

public interface DoctorService {
    List<DoctorVO> list(String name, Long departmentId, Integer status);

    DoctorVO getById(Long id);

    Long create(DoctorSaveDTO doctorSaveDTO);

    void update(Long id, DoctorSaveDTO doctorSaveDTO);

    void updateStatus(Long id, Integer status);
}
