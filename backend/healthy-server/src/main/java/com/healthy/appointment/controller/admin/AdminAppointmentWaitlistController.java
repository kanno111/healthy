package com.healthy.appointment.controller.admin;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.service.AdminAppointmentWaitlistService;
import com.healthy.appointment.vo.AdminAppointmentWaitlistQueueVO;
import com.healthy.appointment.vo.AdminAppointmentWaitlistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Read-only operational views; no endpoint here changes FIFO or waitlist state. */
@RestController
@RequestMapping("/admin/waitlists")
@RequiredArgsConstructor
public class AdminAppointmentWaitlistController {
    private final AdminAppointmentWaitlistService adminAppointmentWaitlistService;

    @GetMapping
    public ApiResponse<PageResult<AdminAppointmentWaitlistVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String patientKeyword,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiResponse.success(adminAppointmentWaitlistService.page(
                status, scheduleDate, departmentId, doctorId, patientKeyword, page, pageSize));
    }

    @GetMapping("/slots/{scheduleSlotId}/queue")
    public ApiResponse<AdminAppointmentWaitlistQueueVO> slotQueue(@PathVariable Long scheduleSlotId) {
        return ApiResponse.success(adminAppointmentWaitlistService.getSlotQueue(scheduleSlotId));
    }
}
