package com.healthy.doctor.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.healthy.doctor.model.DoctorQueryView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Read-only persistence boundary for doctor data. */
public interface DoctorQueryMapper {
    IPage<DoctorQueryView> page(
            @Param("page") IPage<DoctorQueryView> page,
            @Param("name") String name,
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            @Param("departmentStatus") Integer departmentStatus
    );

    DoctorQueryView findById(@Param("id") Long id);

    DoctorQueryView findEnabledByUserId(@Param("userId") Long userId);

    List<DoctorQueryView> listByIds(@Param("doctorIds") List<Long> doctorIds);

    List<Long> findIdsByDepartment(@Param("departmentId") Long departmentId);
}
