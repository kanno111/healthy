package com.healthy.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentCreateDTO {
    @NotNull(message = "请选择预约时段")
    private Long scheduleSlotId;
}
