package com.healthy.appointment.config;

import com.healthy.appointment.interceptor.GatewayIdentityInterceptor;
import com.healthy.appointment.interceptor.DoctorRoleInterceptor;
import com.healthy.appointment.interceptor.PatientRoleInterceptor;
import com.healthy.appointment.interceptor.StaffRoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final GatewayIdentityInterceptor gatewayIdentityInterceptor;
    private final StaffRoleInterceptor staffRoleInterceptor;
    private final PatientRoleInterceptor patientRoleInterceptor;
    private final DoctorRoleInterceptor doctorRoleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayIdentityInterceptor)
                .addPathPatterns("/admin/**", "/user/**", "/doctor/**");
        registry.addInterceptor(staffRoleInterceptor)
                .addPathPatterns("/admin/**");
        registry.addInterceptor(patientRoleInterceptor)
                .addPathPatterns("/user/**");
        registry.addInterceptor(doctorRoleInterceptor)
                .addPathPatterns("/doctor/**");
    }
}
