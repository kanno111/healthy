package com.healthy.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GatewayAuthProperties.class, GatewayJwtProperties.class})
public class GatewaySecurityConfiguration {
}
