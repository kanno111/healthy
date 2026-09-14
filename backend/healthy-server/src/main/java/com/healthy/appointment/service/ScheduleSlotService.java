package com.healthy.appointment.service;

import com.healthy.appointment.dto.ScheduleBatchDTO;
import com.healthy.appointment.vo.ScheduleBatchResultVO;
import com.healthy.appointment.vo.ScheduleSlotVO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleSlotService {
    ScheduleBatchResultVO batchCreate(ScheduleBatchDTO scheduleBatchDTO);

    List<ScheduleSlotVO> list(Long doctorId, LocalDate startDate, LocalDate endDate);

    void updateStatus(Long id, String status);

    void updateCapacity(Long id, Integer capacity);
}
