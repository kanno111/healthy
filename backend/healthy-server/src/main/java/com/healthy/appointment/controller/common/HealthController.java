package com.healthy.appointment.controller.common;

import com.healthy.appointment.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 服务存活检查接口。 */
@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping
    /** 返回服务存活状态，供本地排查和后续健康检查使用。 */
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
