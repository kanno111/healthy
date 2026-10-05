package com.healthy.doctor.controller;

import com.healthy.appointment.dto.DepartmentSaveDTO;
import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.vo.DepartmentVO;
import com.healthy.doctor.service.DepartmentCommandService;
import com.healthy.doctor.service.DepartmentQueryService;
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

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
public class AdminDepartmentController {
    private final DepartmentQueryService departmentQueryService;
    private final DepartmentCommandService departmentCommandService;

    public AdminDepartmentController(
            DepartmentQueryService departmentQueryService,
            DepartmentCommandService departmentCommandService
    ) {
        this.departmentQueryService = departmentQueryService;
        this.departmentCommandService = departmentCommandService;
    }

    @GetMapping
    public ApiResponse<List<DepartmentVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResponse.success(departmentQueryService.list(name, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentVO> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentQueryService.getById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DepartmentSaveDTO request) {
        return ApiResponse.success(departmentCommandService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentSaveDTO request
    ) {
        departmentCommandService.update(id, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        departmentCommandService.updateStatus(id, status);
        return ApiResponse.success();
    }
}
