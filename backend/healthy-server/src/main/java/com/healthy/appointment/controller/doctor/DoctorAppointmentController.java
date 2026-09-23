package com.healthy.appointment.controller.doctor;

import com.healthy.appointment.interceptor.JwtAuthInterceptor;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.DoctorAppointmentService;
import com.healthy.appointment.vo.DoctorAppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/doctor/appointments")
@RequiredArgsConstructor
public class DoctorAppointmentController {
    private final DoctorAppointmentService doctorAppointmentService;

    @GetMapping
    public ApiResponse<PageResult<DoctorAppointmentVO>> pageMine(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(
                doctorAppointmentService.pageMine(userId, scheduleDate, status, page, pageSize));
    }

    @PutMapping("/{appointmentId}/complete")
    public ApiResponse<Void> complete(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable Long appointmentId) {
        doctorAppointmentService.complete(userId, appointmentId);
        return ApiResponse.success();
    }
}
