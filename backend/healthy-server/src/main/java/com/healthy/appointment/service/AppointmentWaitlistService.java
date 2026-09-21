package com.healthy.appointment.service;

import com.healthy.appointment.dto.AppointmentWaitlistCreateDTO;
import com.healthy.appointment.vo.PatientAppointmentWaitlistVO;

import java.util.List;

public interface AppointmentWaitlistService {
    PatientAppointmentWaitlistVO join(Long userId, AppointmentWaitlistCreateDTO appointmentWaitlistCreateDTO);

    List<PatientAppointmentWaitlistVO> listMyWaitlists(Long userId);

    void cancel(Long userId, Long waitlistId);

    /**
     * Locks and offers the first waiting patient for a slot.
     *
     * @return {@code true} when a waiting patient was moved to OFFERED
     */
    boolean offerFirstWaiting(Long scheduleSlotId);

    void confirm(Long userId, Long waitlistId);

    void expireDueOffers();
}
