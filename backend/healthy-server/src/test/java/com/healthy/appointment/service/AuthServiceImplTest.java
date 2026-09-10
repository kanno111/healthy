package com.healthy.appointment.service;

import com.healthy.appointment.config.JwtProperties;
import com.healthy.appointment.config.JwtTokenService;
import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.entity.SysUser;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.mapper.SysUserMapper;
import com.healthy.appointment.service.impl.AuthServiceImpl;
import com.healthy.appointment.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private TokenSessionService tokenSessionService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("healthy-appointment-test-secret-key-must-be-at-least-32-bytes");
        jwtProperties.setTtl(java.time.Duration.ofHours(1));
        authService = new AuthServiceImpl(sysUserMapper, passwordEncoder, new JwtTokenService(jwtProperties), tokenSessionService);
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreCorrect() {
        SysUser user = enabledUser();
        when(sysUserMapper.findEnabledByUsername("patient_demo")).thenReturn(user);

        LoginVO result = authService.login(loginRequest("patient_demo", "123456"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("PATIENT");
        verify(tokenSessionService).save(any(String.class), org.mockito.ArgumentMatchers.eq(1L), any(java.time.Duration.class));
    }

    @Test
    void loginFailsWhenPasswordIsIncorrect() {
        when(sysUserMapper.findEnabledByUsername("patient_demo")).thenReturn(enabledUser());

        assertThatThrownBy(() -> authService.login(loginRequest("patient_demo", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    void logoutRemovesTokenFromRedisSession() {
        authService.logout("test-token");

        verify(tokenSessionService).remove("test-token");
    }

    private SysUser enabledUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("patient_demo");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setName("测试患者");
        user.setRole("PATIENT");
        user.setStatus(1);
        return user;
    }

    private LoginDTO loginRequest(String username, String password) {
        LoginDTO request = new LoginDTO();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
