package com.healthy.appointment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.healthy.appointment.mapper")
public class AppointmentApplication {
    public static void main(String[] args) { SpringApplication.run(AppointmentApplication.class, args); }
}
