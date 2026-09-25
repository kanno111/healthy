package com.healthy.appointment.controller.admin;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.AppointmentService;
import com.healthy.appointment.vo.AdminAppointmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理员对预约就诊状态的操作；/admin/** 已由 StaffRoleInterceptor 保护。 */
@RestController
@RequestMapping("/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageResult<AdminAppointmentVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(appointmentService.pageForAdmin(page, pageSize));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        appointmentService.complete(id);
        return ApiResponse.success();
    }
}
