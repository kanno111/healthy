package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.Department;
import com.healthy.appointment.vo.DepartmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DepartmentMapper {
    List<DepartmentVO> list(@Param("name") String name, @Param("status") Integer status);

    DepartmentVO findById(@Param("id") Long id);

    boolean existsByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    int insert(@Param("department") Department department);

    int update(@Param("department") Department department);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
