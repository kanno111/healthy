package com.healthy.doctor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.healthy.doctor.mapper")
public class HealthyDoctorApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthyDoctorApplication.class, args);
    }
}
