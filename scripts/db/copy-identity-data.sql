-- One-time migration from the former shared schema to identity-service.
-- Preconditions:
--   1. Stop writes to authentication and patient data.
--   2. Create the healthy_identity database.
--   3. Start healthy-identity once so Flyway V1 creates the target tables.
--   4. Run this file before healthy-server applies V13.

START TRANSACTION;

INSERT INTO healthy_identity.sys_user (
    id, username, password_hash, name, phone, role, status, created_at, updated_at
)
SELECT
    id, username, password_hash, name, phone, role, status, created_at, updated_at
FROM healthy.sys_user
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO healthy_identity.patient (
    id, user_id, real_name, gender, birthday, id_card, medical_card_no,
    status, created_at, updated_at
)
SELECT
    id, user_id, real_name, gender, birthday, id_card, medical_card_no,
    status, created_at, updated_at
FROM healthy.patient
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    real_name = VALUES(real_name),
    gender = VALUES(gender),
    birthday = VALUES(birthday),
    id_card = VALUES(id_card),
    medical_card_no = VALUES(medical_card_no),
    status = VALUES(status),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

COMMIT;

-- Every mismatch count must be zero before V13 is allowed to run.
SELECT COUNT(*) AS sys_user_mismatch_count
FROM healthy.sys_user source
LEFT JOIN healthy_identity.sys_user target ON target.id = source.id
WHERE target.id IS NULL
   OR NOT (BINARY target.username <=> BINARY source.username)
   OR NOT (BINARY target.password_hash <=> BINARY source.password_hash)
   OR NOT (BINARY target.name <=> BINARY source.name)
   OR NOT (BINARY target.phone <=> BINARY source.phone)
   OR NOT (BINARY target.role <=> BINARY source.role)
   OR NOT (target.status <=> source.status)
   OR NOT (target.created_at <=> source.created_at)
   OR NOT (target.updated_at <=> source.updated_at);

SELECT COUNT(*) AS patient_mismatch_count
FROM healthy.patient source
LEFT JOIN healthy_identity.patient target ON target.id = source.id
WHERE target.id IS NULL
   OR NOT (target.user_id <=> source.user_id)
   OR NOT (BINARY target.real_name <=> BINARY source.real_name)
   OR NOT (target.gender <=> source.gender)
   OR NOT (target.birthday <=> source.birthday)
   OR NOT (BINARY target.id_card <=> BINARY source.id_card)
   OR NOT (BINARY target.medical_card_no <=> BINARY source.medical_card_no)
   OR NOT (target.status <=> source.status)
   OR NOT (target.created_at <=> source.created_at)
   OR NOT (target.updated_at <=> source.updated_at);

SELECT COUNT(*) AS patient_without_user_count
FROM healthy_identity.patient patient
LEFT JOIN healthy_identity.sys_user account ON account.id = patient.user_id
WHERE account.id IS NULL;

SELECT 'source_sys_user' AS dataset, COUNT(*) AS row_count FROM healthy.sys_user
UNION ALL
SELECT 'target_sys_user', COUNT(*) FROM healthy_identity.sys_user
UNION ALL
SELECT 'source_patient', COUNT(*) FROM healthy.patient
UNION ALL
SELECT 'target_patient', COUNT(*) FROM healthy_identity.patient;
