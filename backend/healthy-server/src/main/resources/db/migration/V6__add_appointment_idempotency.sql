-- Appointment idempotency and status normalization.

ALTER TABLE appointment
    ADD COLUMN request_id VARCHAR(64) NULL AFTER appointment_no;

UPDATE appointment
SET request_id = CONCAT('legacy-', id)
WHERE request_id IS NULL;

UPDATE appointment
SET status = 'BOOKED'
WHERE status = 'CONFIRMED';

ALTER TABLE appointment
    MODIFY COLUMN request_id VARCHAR(64) NOT NULL COMMENT 'idempotency request identifier',
    ADD COLUMN active_booking_key VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'BOOKED' THEN CONCAT(patient_id, ':', schedule_slot_id) ELSE NULL END
        ) STORED COMMENT 'active appointment uniqueness key',
    ADD UNIQUE KEY uk_appointment_request_id (request_id),
    ADD UNIQUE KEY uk_appointment_active_booking (active_booking_key);
