package com.healthy.identity.config;

import com.healthy.identity.interceptor.GatewayIdentityInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class IdentityWebMvcConfig implements WebMvcConfigurer {
    private final GatewayIdentityInterceptor gatewayIdentityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(gatewayIdentityInterceptor)
                .addPathPatterns("/auth/logout");
    }
}
