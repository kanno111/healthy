package com.healthy.appointment.controller.auth;

import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.dto.RegisterDTO;
import com.healthy.appointment.interceptor.JwtAuthInterceptor;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.AuthService;
import com.healthy.appointment.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证相关接口：登录、患者注册与登出。 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    @PostMapping("/login")
    /** 校验账号密码并创建 Redis 会话，返回 JWT 登录凭证。 */
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiResponse.success(authService.login(loginDTO));
    }

    @PostMapping("/register")
    /** 公开患者注册：同时创建 PATIENT 账号和患者档案。 */
    public ApiResponse<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    /** 删除 Redis 中的当前 Token 会话，使该 Token 立即失效。 */
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(jwtAuthInterceptor.extractBearerToken(authorization));
        return ApiResponse.success();
    }
}
