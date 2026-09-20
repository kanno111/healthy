package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.vo.DoctorVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DoctorMapper extends BaseMapper<Doctor> {
    List<DoctorVO> list(@Param("name") String name, @Param("departmentId") Long departmentId, @Param("status") Integer status);

    IPage<DoctorVO> page(
            @Param("page") IPage<DoctorVO> page,
            @Param("name") String name,
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            @Param("departmentStatus") Integer departmentStatus
    );

    DoctorVO findById(@Param("id") Long id);

}
