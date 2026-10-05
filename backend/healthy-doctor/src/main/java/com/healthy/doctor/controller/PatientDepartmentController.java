package com.healthy.doctor.controller;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.vo.PatientDepartmentVO;
import com.healthy.doctor.service.DepartmentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/departments")
public class PatientDepartmentController {
    private final DepartmentQueryService departmentQueryService;

    public PatientDepartmentController(DepartmentQueryService departmentQueryService) {
        this.departmentQueryService = departmentQueryService;
    }

    @GetMapping
    public ApiResponse<List<PatientDepartmentVO>> listVisible() {
        return ApiResponse.success(departmentQueryService.listVisible());
    }
}
