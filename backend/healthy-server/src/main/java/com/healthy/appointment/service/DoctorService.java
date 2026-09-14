package com.healthy.appointment.service;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.DoctorVO;

import java.util.List;

public interface DoctorService {
    List<DoctorVO> list(String name, Long departmentId, Integer status);

    PageResult<DoctorVO> page(String name, Long departmentId, Integer status, int page, int pageSize);

    PageResult<DoctorVO> pageVisible(Long departmentId, String keyword, int page, int pageSize);

    DoctorVO getById(Long id);

    Long create(DoctorSaveDTO doctorSaveDTO);

    void update(Long id, DoctorSaveDTO doctorSaveDTO);

    void updateStatus(Long id, Integer status);
}
