package com.healthy.appointment.controller.admin;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.AppointmentService;
import com.healthy.appointment.vo.AdminAppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员对预约就诊状态的操作；/admin/** 已由 StaffRoleInterceptor 保护。 */
@RestController
@RequestMapping("/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<List<AdminAppointmentVO>> list() {
        return ApiResponse.success(appointmentService.listForAdmin());
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        appointmentService.complete(id);
        return ApiResponse.success();
    }
}
