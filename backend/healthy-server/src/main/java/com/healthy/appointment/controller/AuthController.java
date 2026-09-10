package com.healthy.appointment.controller;

import com.healthy.appointment.dto.LoginDTO;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiResponse.success(authService.login(loginDTO));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(jwtAuthInterceptor.extractBearerToken(authorization));
        return ApiResponse.success();
    }
}
