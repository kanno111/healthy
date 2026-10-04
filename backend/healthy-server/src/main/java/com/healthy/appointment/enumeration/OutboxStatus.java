package com.healthy.appointment.enumeration;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    DEAD
}
