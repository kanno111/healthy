package com.healthy.identity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.healthy.identity.mapper")
public class HealthyIdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthyIdentityApplication.class, args);
    }
}
