package com.healthy.appointment.config;

import com.healthy.appointment.interceptor.JwtAuthInterceptor;
import com.healthy.appointment.interceptor.StaffRoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final StaffRoleInterceptor staffRoleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/auth/register", "/health", "/error");
        registry.addInterceptor(staffRoleInterceptor)
                .addPathPatterns("/admin/**");
    }
}
