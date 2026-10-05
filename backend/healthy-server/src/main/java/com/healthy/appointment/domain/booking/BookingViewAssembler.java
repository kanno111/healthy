package com.healthy.appointment.domain.booking;

import com.healthy.appointment.domain.identity.PatientDirectory;
import com.healthy.appointment.domain.identity.PatientSummary;
import com.healthy.appointment.domain.doctor.DoctorDirectory;
import com.healthy.appointment.domain.doctor.DoctorSummary;
import com.healthy.appointment.vo.AdminAppointmentVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueItemVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistVO;
import com.healthy.appointment.vo.DoctorAppointmentVO;
import com.healthy.appointment.vo.PatientAppointmentVO;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import com.healthy.appointment.vo.ScheduleSlotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BookingViewAssembler {
    private final PatientDirectory patientDirectory;
    private final DoctorDirectory doctorDirectory;

    public PatientAppointmentVO enrich(PatientAppointmentVO appointment) {
        if (appointment == null || appointment.getDoctorId() == null) {
            return appointment;
        }
        applyDoctor(appointment, doctorDirectory.findDoctorsByIds(List.of(appointment.getDoctorId()))
                .get(appointment.getDoctorId()));
        return appointment;
    }

    public List<PatientAppointmentVO> enrichPatientAppointments(List<PatientAppointmentVO> appointments) {
        Map<Long, DoctorSummary> doctors = doctorDirectory.findDoctorsByIds(
                appointments.stream().map(PatientAppointmentVO::getDoctorId).filter(Objects::nonNull).distinct().toList());
        appointments.forEach(appointment -> applyDoctor(appointment, doctors.get(appointment.getDoctorId())));
        return appointments;
    }

    public List<DoctorAppointmentVO> enrichDoctorAppointments(
            List<DoctorAppointmentVO> appointments, DoctorSummary currentDoctor) {
        Map<Long, PatientSummary> patients = patientDirectory.findByIds(
                appointments.stream().map(DoctorAppointmentVO::getPatientId).filter(Objects::nonNull).distinct().toList());
        appointments.forEach(appointment -> {
            PatientSummary patient = patients.get(appointment.getPatientId());
            if (patient != null) {
                appointment.setPatientName(patient.getName());
            }
            if (currentDoctor != null) {
                appointment.setDepartmentName(currentDoctor.getDepartmentName());
            }
        });
        return appointments;
    }

    public List<AdminAppointmentVO> enrichAdminAppointments(List<AdminAppointmentVO> appointments) {
        Map<Long, PatientSummary> patients = patientDirectory.findByIds(
                appointments.stream().map(AdminAppointmentVO::getPatientId).filter(Objects::nonNull).distinct().toList());
        Map<Long, DoctorSummary> doctors = doctorDirectory.findDoctorsByIds(
                appointments.stream().map(AdminAppointmentVO::getDoctorId).filter(Objects::nonNull).distinct().toList());
        appointments.forEach(appointment -> {
            applyPatient(appointment, patients.get(appointment.getPatientId()));
            applyDoctor(appointment, doctors.get(appointment.getDoctorId()));
        });
        return appointments;
    }

    public List<ScheduleSlotVO> enrichScheduleSlots(List<ScheduleSlotVO> slots) {
        Map<Long, DoctorSummary> doctors = doctorDirectory.findDoctorsByIds(
                slots.stream().map(ScheduleSlotVO::getDoctorId).filter(Objects::nonNull).distinct().toList());
        slots.forEach(slot -> {
            DoctorSummary doctor = doctors.get(slot.getDoctorId());
            if (doctor != null) {
                slot.setDoctorName(doctor.getName());
                slot.setDepartmentId(doctor.getDepartmentId());
                slot.setDepartmentName(doctor.getDepartmentName());
            }
        });
        return slots;
    }

    public PatientAppointmentWaitlistVO enrich(PatientAppointmentWaitlistVO waitlist) {
        if (waitlist == null || waitlist.getDoctorId() == null) {
            return waitlist;
        }
        applyDoctor(waitlist, doctorDirectory.findDoctorsByIds(List.of(waitlist.getDoctorId()))
                .get(waitlist.getDoctorId()));
        return waitlist;
    }

    public List<PatientAppointmentWaitlistVO> enrichPatientWaitlists(List<PatientAppointmentWaitlistVO> waitlists) {
        Map<Long, DoctorSummary> doctors = doctorDirectory.findDoctorsByIds(
                waitlists.stream().map(PatientAppointmentWaitlistVO::getDoctorId).filter(Objects::nonNull).distinct().toList());
        waitlists.forEach(waitlist -> applyDoctor(waitlist, doctors.get(waitlist.getDoctorId())));
        return waitlists;
    }

    public List<AdminAppointmentWaitlistVO> enrichAdminWaitlists(List<AdminAppointmentWaitlistVO> waitlists) {
        Map<Long, PatientSummary> patients = patientDirectory.findByIds(
                waitlists.stream().map(AdminAppointmentWaitlistVO::getPatientId).filter(Objects::nonNull).distinct().toList());
        Map<Long, DoctorSummary> doctors = doctorDirectory.findDoctorsByIds(
                waitlists.stream().map(AdminAppointmentWaitlistVO::getDoctorId).filter(Objects::nonNull).distinct().toList());
        waitlists.forEach(waitlist -> {
            PatientSummary patient = patients.get(waitlist.getPatientId());
            if (patient != null) {
                waitlist.setPatientName(patient.getName());
                waitlist.setUsername(patient.getUsername());
                waitlist.setPhone(patient.getPhone());
            }
            DoctorSummary doctor = doctors.get(waitlist.getDoctorId());
            if (doctor != null) {
                waitlist.setDoctorName(doctor.getName());
                waitlist.setDepartmentId(doctor.getDepartmentId());
                waitlist.setDepartmentName(doctor.getDepartmentName());
            }
        });
        return waitlists;
    }

    public AdminAppointmentWaitlistQueueVO enrichQueue(AdminAppointmentWaitlistQueueVO queue) {
        if (queue == null) {
            return null;
        }
        if (queue.getDoctorId() != null) {
            DoctorSummary doctor = doctorDirectory.findDoctorsByIds(List.of(queue.getDoctorId())).get(queue.getDoctorId());
            if (doctor != null) {
                queue.setDoctorName(doctor.getName());
                queue.setDepartmentId(doctor.getDepartmentId());
                queue.setDepartmentName(doctor.getDepartmentName());
            }
        }
        enrichCandidates(queue.getOfferedCandidates());
        enrichCandidates(queue.getWaitingCandidates());
        return queue;
    }

    private void enrichCandidates(Collection<AdminAppointmentWaitlistQueueItemVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Map<Long, PatientSummary> patients = patientDirectory.findByIds(
                candidates.stream().map(AdminAppointmentWaitlistQueueItemVO::getPatientId)
                        .filter(Objects::nonNull).distinct().toList());
        candidates.forEach(candidate -> {
            PatientSummary patient = patients.get(candidate.getPatientId());
            if (patient != null) {
                candidate.setPatientName(patient.getName());
                candidate.setUsername(patient.getUsername());
                candidate.setPhone(patient.getPhone());
            }
        });
    }

    private void applyDoctor(PatientAppointmentVO target, DoctorSummary doctor) {
        if (doctor != null) {
            target.setDoctorName(doctor.getName());
            target.setDepartmentId(doctor.getDepartmentId());
            target.setDepartmentName(doctor.getDepartmentName());
        }
    }

    private void applyDoctor(PatientAppointmentWaitlistVO target, DoctorSummary doctor) {
        if (doctor != null) {
            target.setDoctorName(doctor.getName());
            target.setDepartmentId(doctor.getDepartmentId());
            target.setDepartmentName(doctor.getDepartmentName());
        }
    }

    private void applyPatient(AdminAppointmentVO target, PatientSummary patient) {
        if (patient != null) {
            target.setPatientName(patient.getName());
        }
    }

    private void applyDoctor(AdminAppointmentVO target, DoctorSummary doctor) {
        if (doctor != null) {
            target.setDoctorName(doctor.getName());
            target.setDepartmentId(doctor.getDepartmentId());
            target.setDepartmentName(doctor.getDepartmentName());
        }
    }
}
