-- Supports cleanup scans for active WAITING entries joined to an ended schedule slot.
ALTER TABLE appointment_waitlist
    ADD INDEX idx_waitlist_status_slot_id (status, schedule_slot_id, id);
