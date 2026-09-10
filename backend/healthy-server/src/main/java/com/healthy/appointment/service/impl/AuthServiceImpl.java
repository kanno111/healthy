package com.healthy.appointment.service.impl;

import com.healthy.appointment.config.JwtTokenService;
import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.entity.SysUser;
import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.SysUserMapper;
import com.healthy.appointment.service.AuthService;
import com.healthy.appointment.service.TokenSessionService;
import com.healthy.appointment.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.findEnabledByUsername(loginDTO.getUsername());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        String token = jwtTokenService.createToken(user);
        tokenSessionService.save(token, user.getId(), jwtTokenService.remainingTtl(token));
        return new LoginVO(token, user.getId(), user.getName(), user.getRole());
    }

    @Override
    public void logout(String token) {
        tokenSessionService.remove(token);
    }
}
