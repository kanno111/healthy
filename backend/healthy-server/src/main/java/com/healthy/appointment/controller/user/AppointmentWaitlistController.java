package com.healthy.appointment.controller.user;

import com.healthy.appointment.dto.AppointmentWaitlistCreateDTO;
import com.healthy.appointment.interceptor.JwtAuthInterceptor;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.AppointmentWaitlistService;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/waitlists")
@RequiredArgsConstructor
public class AppointmentWaitlistController {
    private final AppointmentWaitlistService appointmentWaitlistService;

    @PostMapping
    public ApiResponse<PatientAppointmentWaitlistVO> join(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody AppointmentWaitlistCreateDTO appointmentWaitlistCreateDTO
    ) {
        return ApiResponse.success(appointmentWaitlistService.join(userId, appointmentWaitlistCreateDTO));
    }

    @GetMapping
    public ApiResponse<List<PatientAppointmentWaitlistVO>> listMyWaitlists(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId
    ) {
        return ApiResponse.success(appointmentWaitlistService.listMyWaitlists(userId));
    }

    @PatchMapping("/{waitlistId}/cancel")
    public ApiResponse<Void> cancel(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable Long waitlistId
    ) {
        appointmentWaitlistService.cancel(userId, waitlistId);
        return ApiResponse.success();
    }

    @PostMapping("/{waitlistId}/confirm")
    public ApiResponse<Void> confirm(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable Long waitlistId
    ) {
        appointmentWaitlistService.confirm(userId, waitlistId);
        return ApiResponse.success();
    }
}
