package com.healthy.appointment.mapper;

import com.healthy.appointment.entity.SysUser;
import org.apache.ibatis.annotations.Param;

public interface SysUserMapper {
    SysUser findEnabledByUsername(@Param("username") String username);
}
