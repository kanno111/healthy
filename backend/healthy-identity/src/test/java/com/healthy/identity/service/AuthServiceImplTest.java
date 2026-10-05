package com.healthy.identity.service;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.identity.config.JwtProperties;
import com.healthy.identity.config.JwtTokenService;
import com.healthy.identity.dto.LoginDTO;
import com.healthy.identity.dto.RegisterDTO;
import com.healthy.identity.entity.Patient;
import com.healthy.identity.entity.SysUser;
import com.healthy.identity.mapper.PatientMapper;
import com.healthy.identity.mapper.SysUserMapper;
import com.healthy.identity.service.impl.AuthServiceImpl;
import com.healthy.identity.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private TokenSessionService tokenSessionService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("healthy-appointment-test-secret-key-must-be-at-least-32-bytes");
        jwtProperties.setTtl(Duration.ofHours(1));
        authService = new AuthServiceImpl(
                sysUserMapper,
                patientMapper,
                passwordEncoder,
                new JwtTokenService(jwtProperties),
                tokenSessionService);
    }

    @Test
    void loginReturnsTokenAndCreatesRedisSession() {
        SysUser user = enabledUser();
        when(sysUserMapper.selectOne(any())).thenReturn(user);

        LoginVO result = authService.login(loginRequest("patient_demo", "123456"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("PATIENT");
        verify(tokenSessionService).save(any(String.class), eq(1L), any(Duration.class));
    }

    @Test
    void loginRejectsIncorrectPassword() {
        when(sysUserMapper.selectOne(any())).thenReturn(enabledUser());

        assertThatThrownBy(() -> authService.login(loginRequest("patient_demo", "wrong-password")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));
    }

    @Test
    void logoutRemovesRedisSession() {
        authService.logout("test-token");

        verify(tokenSessionService).remove("test-token");
    }

    @Test
    void registrationCreatesPatientAccountAndProfileInOneService() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("patient_new");
        request.setPassword("password123");
        request.setName("New Patient");
        request.setPhone("13800000000");
        request.setGender(1);
        doAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setId(100L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        authService.register(request);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        verify(patientMapper).insert(patientCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo("PATIENT");
        assertThat(passwordEncoder.matches("password123", userCaptor.getValue().getPasswordHash())).isTrue();
        assertThat(patientCaptor.getValue().getUserId()).isEqualTo(100L);
        assertThat(patientCaptor.getValue().getGender()).isEqualTo(1);
    }

    private SysUser enabledUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("patient_demo");
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setName("Test Patient");
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
