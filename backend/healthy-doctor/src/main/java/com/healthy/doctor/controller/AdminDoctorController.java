package com.healthy.doctor.controller;

import com.healthy.appointment.dto.DoctorSaveDTO;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import com.healthy.appointment.vo.DoctorVO;
import com.healthy.doctor.service.DoctorCommandService;
import com.healthy.doctor.service.DoctorQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/doctors")
public class AdminDoctorController {
    private final DoctorQueryService doctorQueryService;
    private final DoctorCommandService doctorCommandService;

    public AdminDoctorController(
            DoctorQueryService doctorQueryService,
            DoctorCommandService doctorCommandService
    ) {
        this.doctorQueryService = doctorQueryService;
        this.doctorCommandService = doctorCommandService;
    }

    @GetMapping
    public ApiResponse<PageResult<DoctorVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status,
            @RequestParam int page,
            @RequestParam int pageSize
    ) {
        return ApiResponse.success(
                doctorQueryService.page(name, departmentId, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorVO> getById(@PathVariable Long id) {
        return ApiResponse.success(doctorQueryService.getAdminById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DoctorSaveDTO request) {
        return ApiResponse.success(doctorCommandService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody DoctorSaveDTO request
    ) {
        doctorCommandService.update(id, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        doctorCommandService.updateStatus(id, status);
        return ApiResponse.success();
    }
}
