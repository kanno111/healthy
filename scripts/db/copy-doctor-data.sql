-- One-time migration from the former shared schema to doctor-service.
-- Preconditions:
--   1. Stop healthy-server, healthy-doctor and healthy-gateway writes.
--   2. Create the healthy_doctor database.
--   3. Start healthy-doctor once so its Flyway V1 creates the target tables.
--   4. Run this file before healthy-server applies V12.

START TRANSACTION;

INSERT INTO healthy_doctor.department (
    id, name, description, sort_order, status, created_at, updated_at
)
SELECT
    id, name, description, sort_order, status, created_at, updated_at
FROM healthy.department
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

INSERT INTO healthy_doctor.doctor (
    id, user_id, name, gender, department_id, doctor_code, title,
    introduction, avatar_url, sort_order, status, created_at, updated_at
)
SELECT
    id, user_id, name, gender, department_id, doctor_code, title,
    introduction, avatar_url, sort_order, status, created_at, updated_at
FROM healthy.doctor
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    name = VALUES(name),
    gender = VALUES(gender),
    department_id = VALUES(department_id),
    doctor_code = VALUES(doctor_code),
    title = VALUES(title),
    introduction = VALUES(introduction),
    avatar_url = VALUES(avatar_url),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

COMMIT;

-- Both mismatch counts must be zero before V12 is allowed to run.
SELECT COUNT(*) AS department_mismatch_count
FROM healthy.department source
LEFT JOIN healthy_doctor.department target ON target.id = source.id
WHERE target.id IS NULL
   OR NOT (target.name <=> source.name)
   OR NOT (target.description <=> source.description)
   OR NOT (target.sort_order <=> source.sort_order)
   OR NOT (target.status <=> source.status)
   OR NOT (target.created_at <=> source.created_at)
   OR NOT (target.updated_at <=> source.updated_at);

SELECT COUNT(*) AS doctor_mismatch_count
FROM healthy.doctor source
LEFT JOIN healthy_doctor.doctor target ON target.id = source.id
WHERE target.id IS NULL
   OR NOT (target.user_id <=> source.user_id)
   OR NOT (target.name <=> source.name)
   OR NOT (target.gender <=> source.gender)
   OR NOT (target.department_id <=> source.department_id)
   OR NOT (target.doctor_code <=> source.doctor_code)
   OR NOT (target.title <=> source.title)
   OR NOT (target.introduction <=> source.introduction)
   OR NOT (target.avatar_url <=> source.avatar_url)
   OR NOT (target.sort_order <=> source.sort_order)
   OR NOT (target.status <=> source.status)
   OR NOT (target.created_at <=> source.created_at)
   OR NOT (target.updated_at <=> source.updated_at);

SELECT 'source_department' AS dataset, COUNT(*) AS row_count FROM healthy.department
UNION ALL
SELECT 'target_department', COUNT(*) FROM healthy_doctor.department
UNION ALL
SELECT 'source_doctor', COUNT(*) FROM healthy.doctor
UNION ALL
SELECT 'target_doctor', COUNT(*) FROM healthy_doctor.doctor;
