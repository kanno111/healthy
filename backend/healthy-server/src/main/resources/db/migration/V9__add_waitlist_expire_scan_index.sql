-- Supports the Scheduled fallback scan:
-- WHERE status = 'OFFERED' AND offer_expire_time <= ? ORDER BY offer_expire_time, id.
ALTER TABLE appointment_waitlist
    ADD INDEX idx_waitlist_status_expire_id (status, offer_expire_time, id);
