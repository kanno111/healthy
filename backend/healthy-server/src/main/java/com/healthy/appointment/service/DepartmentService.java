package com.healthy.appointment.service;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.vo.DepartmentVO;

import java.util.List;

public interface DepartmentService {
    List<DepartmentVO> list(String name, Integer status);

    DepartmentVO getById(Long id);

    Long create(DepartmentSaveDTO departmentSaveDTO);

    void update(Long id, DepartmentSaveDTO departmentSaveDTO);

    void updateStatus(Long id, Integer status);
}
