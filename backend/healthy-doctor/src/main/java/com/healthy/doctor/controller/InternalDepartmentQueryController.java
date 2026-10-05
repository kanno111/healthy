package com.healthy.doctor.controller;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.doctor.service.DoctorQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/departments")
public class InternalDepartmentQueryController {
    private final DoctorQueryService doctorQueryService;

    public InternalDepartmentQueryController(DoctorQueryService doctorQueryService) {
        this.doctorQueryService = doctorQueryService;
    }

    @GetMapping("/{id}/doctor-ids")
    public ApiResponse<List<Long>> findDoctorIds(@PathVariable Long id) {
        return ApiResponse.success(doctorQueryService.findIdsByDepartment(id));
    }
}
