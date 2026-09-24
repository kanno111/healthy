-- Development-only sample data for the Docker MySQL database.
-- Do not place this file in Flyway's db/migration directory.
-- Safe to run repeatedly: stable usernames/codes/numbers and upserts prevent duplicates.

SET NAMES utf8mb4;
START TRANSACTION;

SET @demo_password_hash = '$2b$10$N5q5jGVwpklpaouAvMTM.OLB5Dx0nVafbImR6iOQTbjUXfd78AuL6';
SET @today = CURDATE();
SET @tomorrow = DATE_ADD(@today, INTERVAL 1 DAY);
SET @day_after_tomorrow = DATE_ADD(@today, INTERVAL 2 DAY);
SET @next_week = DATE_ADD(@today, INTERVAL 7 DAY);
SET @two_weeks = DATE_ADD(@today, INTERVAL 13 DAY);

-- All demo accounts use password 123456.
INSERT INTO sys_user (username, password_hash, name, phone, role, status)
VALUES
    ('staff_demo', @demo_password_hash, '演示管理员', '13900000001', 'STAFF', 1),
    ('doctor_cardio', @demo_password_hash, '林知远', '13900000011', 'DOCTOR', 1),
    ('doctor_digestive', @demo_password_hash, '陈书宁', '13900000012', 'DOCTOR', 1),
    ('doctor_pediatrics', @demo_password_hash, '苏念安', '13900000013', 'DOCTOR', 1),
    ('patient_demo', @demo_password_hash, '张小明', '13800000001', 'PATIENT', 1),
    ('patient_wang', @demo_password_hash, '王女士', '13800000002', 'PATIENT', 1),
    ('patient_li', @demo_password_hash, '李先生', '13800000003', 'PATIENT', 1),
    ('patient_zhao', @demo_password_hash, '赵女士', '13800000004', 'PATIENT', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    role = VALUES(role),
    status = VALUES(status);

SET @staff_user_id = (SELECT id FROM sys_user WHERE username = 'staff_demo');
SET @cardio_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_cardio');
SET @digestive_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_digestive');
SET @pediatrics_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_pediatrics');
SET @patient_demo_user_id = (SELECT id FROM sys_user WHERE username = 'patient_demo');
SET @patient_wang_user_id = (SELECT id FROM sys_user WHERE username = 'patient_wang');
SET @patient_li_user_id = (SELECT id FROM sys_user WHERE username = 'patient_li');
SET @patient_zhao_user_id = (SELECT id FROM sys_user WHERE username = 'patient_zhao');

INSERT INTO department (name, description, sort_order, status)
VALUES
    ('心血管内科', '提供常见心血管疾病门诊咨询与复诊服务。', 10, 1),
    ('消化内科', '提供消化系统常见疾病门诊咨询服务。', 20, 1),
    ('儿科', '提供儿童常见疾病门诊咨询服务。', 30, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

SET @cardio_department_id = (SELECT id FROM department WHERE name = '心血管内科');
SET @digestive_department_id = (SELECT id FROM department WHERE name = '消化内科');
SET @pediatrics_department_id = (SELECT id FROM department WHERE name = '儿科');

INSERT INTO patient (user_id, real_name, gender, birthday, id_card, medical_card_no, status)
VALUES
    (@patient_demo_user_id, '张小明', 1, '1995-05-12', NULL, 'DEMO-MC-001', 1),
    (@patient_wang_user_id, '王女士', 2, '1988-11-03', NULL, 'DEMO-MC-002', 1),
    (@patient_li_user_id, '李先生', 1, '1979-02-18', NULL, 'DEMO-MC-003', 1),
    (@patient_zhao_user_id, '赵女士', 2, '2001-08-26', NULL, 'DEMO-MC-004', 1)
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    gender = VALUES(gender),
    status = VALUES(status);

SET @patient_demo_id = (SELECT id FROM patient WHERE user_id = @patient_demo_user_id);
SET @patient_wang_id = (SELECT id FROM patient WHERE user_id = @patient_wang_user_id);
SET @patient_li_id = (SELECT id FROM patient WHERE user_id = @patient_li_user_id);
SET @patient_zhao_id = (SELECT id FROM patient WHERE user_id = @patient_zhao_user_id);

INSERT INTO doctor (
    user_id, name, gender, department_id, doctor_code, title,
    introduction, avatar_url, sort_order, status
)
VALUES
    (@cardio_user_id, '林知远', 1, @cardio_department_id, 'DOC-CARDIO-001', '主任医师',
     '擅长高血压、冠心病等常见心血管疾病的门诊诊疗。', NULL, 10, 1),
    (@digestive_user_id, '陈书宁', 2, @digestive_department_id, 'DOC-DIGEST-001', '副主任医师',
     '擅长胃肠道常见疾病和慢性消化系统疾病管理。', NULL, 20, 1),
    (@pediatrics_user_id, '苏念安', 2, @pediatrics_department_id, 'DOC-PEDS-001', '主治医师',
     '擅长儿童呼吸道及消化道常见疾病诊疗。', NULL, 30, 1)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    name = VALUES(name),
    department_id = VALUES(department_id),
    title = VALUES(title),
    introduction = VALUES(introduction),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

SET @cardio_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-CARDIO-001');
SET @digestive_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-DIGEST-001');
SET @pediatrics_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-PEDS-001');

INSERT INTO doctor_schedule_slot (
    doctor_id, schedule_date, session_type, session_name, start_time, end_time,
    average_consultation_minutes, total_capacity, remaining_capacity,
    next_queue_number, status, version
)
VALUES
    (@cardio_doctor_id, @today, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 20, 6, 4, 3, 'OPEN', 0),
    (@digestive_doctor_id, @today, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 5, 4, 2, 'OPEN', 0),
    (@pediatrics_doctor_id, @today, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 15, 4, 3, 2, 'OPEN', 0),
    (@cardio_doctor_id, @tomorrow, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 20, 8, 8, 1, 'OPEN', 0),
    (@digestive_doctor_id, @tomorrow, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 20, 2, 0, 3, 'OPEN', 0),
    (@pediatrics_doctor_id, @day_after_tomorrow, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 15, 6, 6, 1, 'OPEN', 0),
    (@cardio_doctor_id, @next_week, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 8, 8, 1, 'OPEN', 0),
    (@digestive_doctor_id, @two_weeks, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 6, 6, 1, 'OPEN', 0)
ON DUPLICATE KEY UPDATE
    session_type = VALUES(session_type),
    session_name = VALUES(session_name),
    status = VALUES(status);

SET @cardio_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @cardio_doctor_id AND schedule_date = @today
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @digestive_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @digestive_doctor_id AND schedule_date = @today
      AND start_time = '14:00:00' AND end_time = '17:00:00'
);
SET @pediatrics_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @pediatrics_doctor_id AND schedule_date = @today
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @digestive_tomorrow_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @digestive_doctor_id AND schedule_date = @tomorrow
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @pediatrics_future_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @pediatrics_doctor_id AND schedule_date = @day_after_tomorrow
      AND start_time = '14:00:00' AND end_time = '17:00:00'
);

INSERT INTO appointment (
    appointment_no, request_id, patient_id, doctor_id, schedule_slot_id,
    queue_number, schedule_date, start_time, end_time, estimated_arrival_time,
    status, cancelled_at, cancel_reason, created_at
)
VALUES
    ('DEMO-APT-0001', 'demo-request-0001', @patient_demo_id, @cardio_doctor_id,
     @cardio_today_slot, 1, @today, '08:00:00', '12:00:00', NULL,
     'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('DEMO-APT-0002', 'demo-request-0002', @patient_wang_id, @cardio_doctor_id,
     @cardio_today_slot, 2, @today, '08:00:00', '12:00:00', NULL,
     'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('DEMO-APT-0003', 'demo-request-0003', @patient_li_id, @pediatrics_doctor_id,
     @pediatrics_today_slot, 1, @today, '08:00:00', '12:00:00', NULL,
     'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('DEMO-APT-0004', 'demo-request-0004', @patient_demo_id, @digestive_doctor_id,
     @digestive_today_slot, 1, @today, '14:00:00', '17:00:00', NULL,
     'CANCELLED', DATE_SUB(NOW(), INTERVAL 6 HOUR), '患者主动取消', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    ('DEMO-APT-0005', 'demo-request-0005', @patient_wang_id, @digestive_doctor_id,
     @digestive_today_slot, 1, @today, '14:00:00', '17:00:00', NULL,
     'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    ('DEMO-APT-0006', 'demo-request-0006', @patient_demo_id, @digestive_doctor_id,
     @digestive_tomorrow_slot, 1, @tomorrow, '08:00:00', '12:00:00', NULL,
     'BOOKED', NULL, NULL, NOW()),
    ('DEMO-APT-0007', 'demo-request-0007', @patient_li_id, @digestive_doctor_id,
     @digestive_tomorrow_slot, 2, @tomorrow, '08:00:00', '12:00:00', NULL,
     'BOOKED', NULL, NULL, NOW())
ON DUPLICATE KEY UPDATE
    appointment_no = VALUES(appointment_no);

INSERT INTO appointment_waitlist (
    patient_id, schedule_slot_id, status, offer_expire_time, created_at
)
VALUES
    (@patient_zhao_id, @digestive_tomorrow_slot, 'WAITING', NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (@patient_wang_id, @digestive_tomorrow_slot, 'OFFERED', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE))
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    offer_expire_time = VALUES(offer_expire_time);

INSERT INTO appointment_waitlist (
    patient_id, schedule_slot_id, status, offer_expire_time, created_at
)
SELECT @patient_demo_id, @digestive_tomorrow_slot, 'CONFIRMED', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM appointment_waitlist
    WHERE patient_id = @patient_demo_id
      AND schedule_slot_id = @digestive_tomorrow_slot
      AND status = 'CONFIRMED'
);

INSERT INTO appointment_waitlist (
    patient_id, schedule_slot_id, status, offer_expire_time, created_at
)
SELECT @patient_li_id, @pediatrics_future_slot, 'CANCELLED', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM appointment_waitlist
    WHERE patient_id = @patient_li_id
      AND schedule_slot_id = @pediatrics_future_slot
      AND status = 'CANCELLED'
);

COMMIT;
