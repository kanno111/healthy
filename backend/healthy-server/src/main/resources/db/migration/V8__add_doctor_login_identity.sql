ALTER TABLE sys_user
    DROP CHECK chk_sys_user_role,
    ADD CONSTRAINT chk_sys_user_role CHECK (role IN ('PATIENT', 'STAFF', 'DOCTOR'));

ALTER TABLE doctor
    ADD COLUMN user_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'Logical link to the doctor login account' AFTER id,
    ADD UNIQUE KEY uk_doctor_user_id (user_id);

UPDATE doctor doctor_record
INNER JOIN sys_user account
        ON account.username COLLATE utf8mb4_unicode_ci = doctor_record.doctor_code
       AND account.role = 'DOCTOR'
SET doctor_record.user_id = account.id
WHERE doctor_record.user_id IS NULL;
