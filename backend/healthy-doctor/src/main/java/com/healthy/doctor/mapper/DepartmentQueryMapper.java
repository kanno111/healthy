package com.healthy.doctor.mapper;

import com.healthy.appointment.vo.DepartmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Read-only persistence boundary for department data. */
public interface DepartmentQueryMapper {
    List<DepartmentVO> list(@Param("name") String name, @Param("status") Integer status);

    DepartmentVO findById(@Param("id") Long id);
}
