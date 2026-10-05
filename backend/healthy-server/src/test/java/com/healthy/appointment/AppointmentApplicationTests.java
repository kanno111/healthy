package com.healthy.appointment;

import com.healthy.appointment.domain.doctor.DoctorDirectory;
import com.healthy.appointment.domain.doctor.remote.FeignDoctorDirectory;
import com.healthy.appointment.domain.identity.PatientDirectory;
import com.healthy.appointment.domain.identity.remote.FeignPatientDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppointmentApplicationTests {
    @Autowired
    private DoctorDirectory doctorDirectory;

    @Autowired
    private PatientDirectory patientDirectory;

    @Test
    void defaultsToFeignDoctorDirectory() {
        assertThat(doctorDirectory).isInstanceOf(FeignDoctorDirectory.class);
    }

    @Test
    void defaultsToFeignPatientDirectory() {
        assertThat(patientDirectory).isInstanceOf(FeignPatientDirectory.class);
    }
}
