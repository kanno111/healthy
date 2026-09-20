package com.healthy.appointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.healthy.appointment.config.JwtTokenService;
import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.dto.RegisterDTO;
import com.healthy.appointment.entity.Patient;
import com.healthy.appointment.entity.SysUser;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.SysUserMapper;
import com.healthy.appointment.mapper.PatientMapper;
import com.healthy.appointment.service.AuthService;
import com.healthy.appointment.service.TokenSessionService;
import com.healthy.appointment.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SysUserMapper sysUserMapper;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername())
                .eq(SysUser::getStatus, 1)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        String token = jwtTokenService.createToken(user);
        tokenSessionService.save(token, user.getId(), jwtTokenService.remainingTtl(token));
        return new LoginVO(token, user.getId(), user.getName(), user.getRole());
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (sysUserMapper.exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, registerDTO.getUsername()))) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (sysUserMapper.exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, registerDTO.getPhone()))) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setName(registerDTO.getName());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);
        // 公开入口只允许创建患者账号；管理员由管理端创建。
        user.setRole("PATIENT");
        sysUserMapper.insert(user);

        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setRealName(registerDTO.getName());
        patient.setGender(registerDTO.getGender());
        patient.setStatus(1);
        patientMapper.insert(patient);
    }

    @Override
    public void logout(String token) {
        tokenSessionService.remove(token);
    }
}
