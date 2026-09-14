package com.healthy.appointment.controller.user;

import com.healthy.appointment.dto.AppointmentCreateDTO;
import com.healthy.appointment.interceptor.JwtAuthInterceptor;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.AppointmentService;
import com.healthy.appointment.vo.PatientAppointmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    /** 使用当前登录患者的身份创建预约，并扣减所选班次的剩余号源。 */
    public ApiResponse<PatientAppointmentVO> create(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody AppointmentCreateDTO appointmentCreateDTO
    ) {
        return ApiResponse.success(appointmentService.create(userId, appointmentCreateDTO));
    }

    @GetMapping
    /** 查询当前登录患者的预约记录，按创建时间倒序返回。 */
    public ApiResponse<List<PatientAppointmentVO>> listMyAppointments(
            @RequestAttribute(JwtAuthInterceptor.CURRENT_USER_ID) Long userId
    ) {
        return ApiResponse.success(appointmentService.listMyAppointments(userId));
    }
}
