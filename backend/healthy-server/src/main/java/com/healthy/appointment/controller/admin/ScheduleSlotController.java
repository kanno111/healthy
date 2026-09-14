package com.healthy.appointment.controller.admin;

import com.healthy.appointment.dto.ScheduleBatchDTO;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.service.ScheduleSlotService;
import com.healthy.appointment.vo.ScheduleBatchResultVO;
import com.healthy.appointment.vo.ScheduleSlotVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/schedule-slots")
@RequiredArgsConstructor
public class ScheduleSlotController {
    private final ScheduleSlotService scheduleSlotService;

    @PostMapping("/batch")
    /** 根据日期范围、出诊星期和时段规则，事务化批量生成医生可预约号源。 */
    public ApiResponse<ScheduleBatchResultVO> batchCreate(@Valid @RequestBody ScheduleBatchDTO scheduleBatchDTO) {
        return ApiResponse.success(scheduleSlotService.batchCreate(scheduleBatchDTO));
    }

    @GetMapping
    /** 查询指定日期范围内的排班，以及每个班次的总号源、已预约和剩余号源。 */
    public ApiResponse<List<ScheduleSlotVO>> list(
            @RequestParam(required = false) Long doctorId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ApiResponse.success(scheduleSlotService.list(doctorId, startDate, endDate));
    }

    @PatchMapping("/{id}/status")
    /** 开放或关闭指定出诊班次；关闭后患者不能继续预约。 */
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        scheduleSlotService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/capacity")
    /** 调整班次总号源；新的总号源不能小于当前已预约人数。 */
    public ApiResponse<Void> updateCapacity(@PathVariable Long id, @RequestParam Integer capacity) {
        scheduleSlotService.updateCapacity(id, capacity);
        return ApiResponse.success();
    }
}
