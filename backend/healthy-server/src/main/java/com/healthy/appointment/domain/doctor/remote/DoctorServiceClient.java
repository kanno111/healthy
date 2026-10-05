package com.healthy.appointment.domain.doctor.remote;

import com.healthy.appointment.result.ApiResponse;
import com.healthy.appointment.result.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        contextId = "doctorServiceClient",
        name = "healthy-doctor",
        path = "/api/internal"
)
public interface DoctorServiceClient {
    @GetMapping("/doctors/{id}")
    ApiResponse<DoctorRemoteView> getDoctor(@PathVariable("id") Long id);

    @GetMapping("/doctors/visible")
    ApiResponse<PageResult<DoctorRemoteView>> pageVisibleDoctors(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize
    );

    @GetMapping("/doctors/by-user/{userId}")
    ApiResponse<DoctorRemoteView> getEnabledDoctorByUserId(@PathVariable("userId") Long userId);

    @PostMapping("/doctors/batch")
    ApiResponse<List<DoctorRemoteView>> listDoctorsByIds(@RequestBody List<Long> doctorIds);

    @GetMapping("/departments/{departmentId}/doctor-ids")
    ApiResponse<List<Long>> listDoctorIdsByDepartment(@PathVariable("departmentId") Long departmentId);
}
