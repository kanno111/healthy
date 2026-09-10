package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.SysUser;
import org.apache.ibatis.annotations.Param;

public interface SysUserMapper {
    SysUser findEnabledByUsername(@Param("username") String username);

    boolean existsByUsername(@Param("username") String username);

    boolean existsByPhone(@Param("phone") String phone);

    int insert(@Param("user") SysUser user);
}
