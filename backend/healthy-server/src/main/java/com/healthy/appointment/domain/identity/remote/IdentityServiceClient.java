package com.healthy.appointment.domain.identity.remote;

import com.healthy.appointment.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        contextId = "identityServiceClient",
        name = "healthy-identity",
        path = "/api/internal/patients"
)
public interface IdentityServiceClient {
    @GetMapping("/by-user/{userId}")
    ApiResponse<PatientRemoteView> getEnabledPatientByUserId(@PathVariable("userId") Long userId);

    @PostMapping("/batch")
    ApiResponse<List<PatientRemoteView>> listPatientsByIds(@RequestBody List<Long> patientIds);

    @GetMapping("/search")
    ApiResponse<List<Long>> findPatientIdsByKeyword(@RequestParam("keyword") String keyword);
}
