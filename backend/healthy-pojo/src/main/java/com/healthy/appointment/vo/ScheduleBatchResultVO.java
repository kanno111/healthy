package com.healthy.appointment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ScheduleBatchResultVO {
    private Integer createdCount;
    private List<ScheduleSlotVO> slots;
}
