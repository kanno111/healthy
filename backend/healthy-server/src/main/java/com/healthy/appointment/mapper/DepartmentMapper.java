package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.healthy.appointment.entity.Department;
import com.healthy.appointment.vo.DepartmentVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DepartmentMapper extends BaseMapper<Department> {
    List<DepartmentVO> list(@Param("name") String name, @Param("status") Integer status);

    DepartmentVO findById(@Param("id") Long id);

}
