package com.healthy.doctor.config;

import com.healthy.doctor.interceptor.GatewayIdentityInterceptor;
import com.healthy.doctor.interceptor.GatewayPatientRoleInterceptor;
import com.healthy.doctor.interceptor.GatewayStaffRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DoctorWebMvcConfig implements WebMvcConfigurer {
    private final GatewayIdentityInterceptor gatewayIdentityInterceptor;
    private final GatewayStaffRoleInterceptor gatewayStaffRoleInterceptor;
    private final GatewayPatientRoleInterceptor gatewayPatientRoleInterceptor;

    public DoctorWebMvcConfig(
            GatewayIdentityInterceptor gatewayIdentityInterceptor,
            GatewayStaffRoleInterceptor gatewayStaffRoleInterceptor,
            GatewayPatientRoleInterceptor gatewayPatientRoleInterceptor
    ) {
        this.gatewayIdentityInterceptor = gatewayIdentityInterceptor;
        this.gatewayStaffRoleInterceptor = gatewayStaffRoleInterceptor;
        this.gatewayPatientRoleInterceptor = gatewayPatientRoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayIdentityInterceptor)
                .addPathPatterns("/admin/**", "/user/**");
        registry.addInterceptor(gatewayStaffRoleInterceptor)
                .addPathPatterns("/admin/**");
        registry.addInterceptor(gatewayPatientRoleInterceptor)
                .addPathPatterns("/user/**");
    }
}
