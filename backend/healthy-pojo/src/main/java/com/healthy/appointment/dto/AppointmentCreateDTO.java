package com.healthy.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentCreateDTO {
    @NotNull(message = "请选择预约时段")
    private Long scheduleSlotId;

    /** 同一次提交及其网络重试必须使用同一个请求标识。 */
    @NotBlank(message = "请求标识不能为空")
    @Size(max = 64, message = "请求标识长度不能超过64个字符")
    private String requestId;
}
