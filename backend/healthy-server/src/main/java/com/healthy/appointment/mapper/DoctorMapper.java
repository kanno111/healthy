package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.Doctor;
import com.healthy.appointment.vo.DoctorVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DoctorMapper {
    List<DoctorVO> list(@Param("name") String name, @Param("departmentId") Long departmentId, @Param("status") Integer status);

    long count(
            @Param("name") String name,
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            @Param("departmentStatus") Integer departmentStatus
    );

    List<DoctorVO> page(
            @Param("name") String name,
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            @Param("departmentStatus") Integer departmentStatus,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    DoctorVO findById(@Param("id") Long id);

    boolean existsByDoctorCode(@Param("doctorCode") String doctorCode, @Param("excludeId") Long excludeId);

    int insert(@Param("doctor") Doctor doctor);

    int update(@Param("doctor") Doctor doctor);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
