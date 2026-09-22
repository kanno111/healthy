package com.healthy.appointment.mq;

/** Delayed-message payload; database state remains the only source of truth. */
public record WaitlistTimeoutMessage(Long waitlistId, Long scheduleSlotId) {
}
