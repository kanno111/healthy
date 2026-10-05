package com.healthy.appointment.domain.booking;

import com.healthy.appointment.domain.identity.PatientDirectory;
import com.healthy.appointment.domain.identity.PatientSummary;
import com.healthy.appointment.domain.provider.DoctorSummary;
import com.healthy.appointment.domain.provider.ProviderDirectory;
import com.healthy.appointment.vo.AdminAppointmentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingViewAssemblerTest {
    @Mock
    private PatientDirectory patientDirectory;
    @Mock
    private ProviderDirectory providerDirectory;

    @Test
    void enrichesPageWithOneBatchLookupPerRemoteDomain() {
        BookingViewAssembler assembler = new BookingViewAssembler(patientDirectory, providerDirectory);
        AdminAppointmentVO first = appointment(1L, 11L);
        AdminAppointmentVO second = appointment(2L, 11L);
        when(patientDirectory.findByIds(anyCollection())).thenReturn(Map.of(
                1L, patient(1L, "Alice"),
                2L, patient(2L, "Bob")));
        when(providerDirectory.findDoctorsByIds(anyCollection())).thenReturn(Map.of(
                11L, doctor(11L, "Dr Zhang", 3L, "Cardiology")));

        List<AdminAppointmentVO> result = assembler.enrichAdminAppointments(List.of(first, second));

        assertThat(result).extracting(AdminAppointmentVO::getPatientName).containsExactly("Alice", "Bob");
        assertThat(result).extracting(AdminAppointmentVO::getDoctorName).containsOnly("Dr Zhang");
        assertThat(result).extracting(AdminAppointmentVO::getDepartmentName).containsOnly("Cardiology");
        verify(patientDirectory, times(1)).findByIds(anyCollection());
        verify(providerDirectory, times(1)).findDoctorsByIds(anyCollection());
    }

    private AdminAppointmentVO appointment(Long patientId, Long doctorId) {
        AdminAppointmentVO appointment = new AdminAppointmentVO();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        return appointment;
    }

    private PatientSummary patient(Long id, String name) {
        PatientSummary patient = new PatientSummary();
        patient.setId(id);
        patient.setName(name);
        return patient;
    }

    private DoctorSummary doctor(Long id, String name, Long departmentId, String departmentName) {
        DoctorSummary doctor = new DoctorSummary();
        doctor.setId(id);
        doctor.setName(name);
        doctor.setDepartmentId(departmentId);
        doctor.setDepartmentName(departmentName);
        return doctor;
    }
}
