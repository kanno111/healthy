CREATE TABLE appointment_waitlist (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    patient_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to patient.id',
    schedule_slot_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to doctor_schedule_slot.id',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING, OFFERED, CONFIRMED, EXPIRED, CANCELLED',
    offer_expire_time DATETIME DEFAULT NULL COMMENT 'offer confirmation deadline',
    active_waitlist_key VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('WAITING', 'OFFERED') THEN CONCAT(patient_id, ':', schedule_slot_id)
                ELSE NULL
            END
        ) STORED COMMENT 'unique key for an active waitlist entry',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_waitlist_active (active_waitlist_key),
    KEY idx_waitlist_slot_status_created (schedule_slot_id, status, created_at, id),
    KEY idx_waitlist_patient_created (patient_id, created_at),
    CONSTRAINT chk_waitlist_status CHECK (status IN ('WAITING', 'OFFERED', 'CONFIRMED', 'EXPIRED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='appointment waitlist';
