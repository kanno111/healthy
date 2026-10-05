package com.healthy.identity.controller;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.identity.model.PatientQueryView;
import com.healthy.identity.service.PatientQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
public class InternalPatientQueryController {
    private final PatientQueryService patientQueryService;

    @GetMapping("/by-user/{userId}")
    public ApiResponse<PatientQueryView> findEnabledByUserId(@PathVariable Long userId) {
        return ApiResponse.success(patientQueryService.requireEnabledByUserId(userId));
    }

    @PostMapping("/batch")
    public ApiResponse<List<PatientQueryView>> findByIds(@RequestBody List<Long> patientIds) {
        return ApiResponse.success(patientQueryService.findByIds(patientIds));
    }

    @GetMapping("/search")
    public ApiResponse<List<Long>> findIdsByKeyword(@RequestParam String keyword) {
        return ApiResponse.success(patientQueryService.findIdsByKeyword(keyword));
    }
}
