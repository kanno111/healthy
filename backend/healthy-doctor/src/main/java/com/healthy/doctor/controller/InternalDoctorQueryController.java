package com.healthy.doctor.controller;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.doctor.model.DoctorQueryView;
import com.healthy.doctor.service.DoctorQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/doctors")
public class InternalDoctorQueryController {
    private final DoctorQueryService doctorQueryService;

    public InternalDoctorQueryController(DoctorQueryService doctorQueryService) {
        this.doctorQueryService = doctorQueryService;
    }

    @GetMapping("/visible")
    public ApiResponse<PageResult<DoctorVO>> pageVisible(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiResponse.success(doctorQueryService.pageVisible(departmentId, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorQueryView> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorQueryService.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    public ApiResponse<DoctorQueryView> getEnabledByUserId(@PathVariable Long userId) {
        return ApiResponse.success(doctorQueryService.getEnabledByUserId(userId));
    }

    @PostMapping("/batch")
    public ApiResponse<List<DoctorQueryView>> listByIds(@RequestBody List<Long> doctorIds) {
        return ApiResponse.success(doctorQueryService.listByIds(doctorIds));
    }
}
