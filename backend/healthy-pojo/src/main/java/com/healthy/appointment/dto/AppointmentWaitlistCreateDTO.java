package com.healthy.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentWaitlistCreateDTO {
    @NotNull(message = "请选择候补班次")
    private Long scheduleSlotId;
}
