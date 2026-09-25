-- Development-only sample data for the Docker MySQL database.
-- Do not place this file in Flyway's db/migration directory.
-- Safe to run repeatedly: stable usernames/codes/numbers and upserts prevent duplicates.

SET NAMES utf8mb4;
START TRANSACTION;

SET @demo_password_hash = '$2b$10$N5q5jGVwpklpaouAvMTM.OLB5Dx0nVafbImR6iOQTbjUXfd78AuL6';
SET @today = CURDATE();
SET @yesterday = DATE_SUB(@today, INTERVAL 1 DAY);
SET @tomorrow = DATE_ADD(@today, INTERVAL 1 DAY);
SET @day_after_tomorrow = DATE_ADD(@today, INTERVAL 2 DAY);
SET @next_week = DATE_ADD(@today, INTERVAL 7 DAY);
SET @two_weeks = DATE_ADD(@today, INTERVAL 13 DAY);
SET @demo_date_key = DATE_FORMAT(@today, '%Y%m%d');

-- All demo accounts use password 123456.
INSERT INTO sys_user (username, password_hash, name, phone, role, status)
VALUES
    ('staff_demo', @demo_password_hash, '演示管理员', '19990000001', 'STAFF', 1),
    ('doctor_cardio', @demo_password_hash, '林知远', '19990000011', 'DOCTOR', 1),
    ('doctor_cardio_2', @demo_password_hash, '周景川', '19990000101', 'DOCTOR', 1),
    ('doctor_digestive', @demo_password_hash, '陈书宁', '19990000012', 'DOCTOR', 1),
    ('doctor_digestive_2', @demo_password_hash, '刘志强', '19990000102', 'DOCTOR', 1),
    ('doctor_pediatrics', @demo_password_hash, '苏念安', '19990000013', 'DOCTOR', 1),
    ('doctor_respiratory', @demo_password_hash, '徐明哲', '19990000103', 'DOCTOR', 1),
    ('doctor_neurology', @demo_password_hash, '何雨桐', '19990000104', 'DOCTOR', 1),
    ('doctor_orthopedics', @demo_password_hash, '顾承安', '19990000105', 'DOCTOR', 1),
    ('doctor_dermatology', @demo_password_hash, '方可欣', '19990000106', 'DOCTOR', 1),
    ('doctor_ophthalmology', @demo_password_hash, '沈清和', '19990000107', 'DOCTOR', 1),
    ('patient_demo', @demo_password_hash, '张小明', '19890000001', 'PATIENT', 1),
    ('patient_wang', @demo_password_hash, '王女士', '19890000002', 'PATIENT', 1),
    ('patient_li', @demo_password_hash, '李先生', '19890000003', 'PATIENT', 1),
    ('patient_zhao', @demo_password_hash, '赵女士', '19890000004', 'PATIENT', 1),
    ('patient_chen', @demo_password_hash, '陈晨', '19890000101', 'PATIENT', 1),
    ('patient_zhou', @demo_password_hash, '周然', '19890000102', 'PATIENT', 1),
    ('patient_sun', @demo_password_hash, '孙悦', '19890000103', 'PATIENT', 1),
    ('patient_wu', @demo_password_hash, '吴桐', '19890000104', 'PATIENT', 1),
    ('patient_zheng', @demo_password_hash, '郑宇', '19890000105', 'PATIENT', 1),
    ('patient_feng', @demo_password_hash, '冯雪', '19890000106', 'PATIENT', 1),
    ('patient_he', @demo_password_hash, '何嘉', '19890000107', 'PATIENT', 1),
    ('patient_luo', @demo_password_hash, '罗宁', '19890000108', 'PATIENT', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    role = VALUES(role),
    status = VALUES(status);

SET @staff_user_id = (SELECT id FROM sys_user WHERE username = 'staff_demo');
SET @cardio_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_cardio');
SET @cardio_2_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_cardio_2');
SET @digestive_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_digestive');
SET @digestive_2_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_digestive_2');
SET @pediatrics_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_pediatrics');
SET @respiratory_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_respiratory');
SET @neurology_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_neurology');
SET @orthopedics_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_orthopedics');
SET @dermatology_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_dermatology');
SET @ophthalmology_user_id = (SELECT id FROM sys_user WHERE username = 'doctor_ophthalmology');
SET @patient_demo_user_id = (SELECT id FROM sys_user WHERE username = 'patient_demo');
SET @patient_wang_user_id = (SELECT id FROM sys_user WHERE username = 'patient_wang');
SET @patient_li_user_id = (SELECT id FROM sys_user WHERE username = 'patient_li');
SET @patient_zhao_user_id = (SELECT id FROM sys_user WHERE username = 'patient_zhao');
SET @patient_chen_user_id = (SELECT id FROM sys_user WHERE username = 'patient_chen');
SET @patient_zhou_user_id = (SELECT id FROM sys_user WHERE username = 'patient_zhou');
SET @patient_sun_user_id = (SELECT id FROM sys_user WHERE username = 'patient_sun');
SET @patient_wu_user_id = (SELECT id FROM sys_user WHERE username = 'patient_wu');
SET @patient_zheng_user_id = (SELECT id FROM sys_user WHERE username = 'patient_zheng');
SET @patient_feng_user_id = (SELECT id FROM sys_user WHERE username = 'patient_feng');
SET @patient_he_user_id = (SELECT id FROM sys_user WHERE username = 'patient_he');
SET @patient_luo_user_id = (SELECT id FROM sys_user WHERE username = 'patient_luo');

INSERT INTO department (name, description, sort_order, status)
VALUES
    ('心血管内科', '提供常见心血管疾病门诊咨询与复诊服务。', 10, 1),
    ('消化内科', '提供消化系统常见疾病门诊咨询服务。', 20, 1),
    ('儿科', '提供儿童常见疾病门诊咨询服务。', 30, 1),
    ('呼吸内科', '提供呼吸系统常见疾病门诊咨询与慢病管理。', 40, 1),
    ('神经内科', '提供头痛、眩晕及脑血管疾病门诊服务。', 50, 1),
    ('骨科', '提供骨关节、脊柱及运动损伤门诊服务。', 60, 1),
    ('皮肤科', '提供常见皮肤疾病与过敏问题门诊服务。', 70, 1),
    ('眼科', '提供常见眼病、视力检查与眼健康咨询。', 80, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

SET @cardio_department_id = (SELECT id FROM department WHERE name = '心血管内科');
SET @digestive_department_id = (SELECT id FROM department WHERE name = '消化内科');
SET @pediatrics_department_id = (SELECT id FROM department WHERE name = '儿科');
SET @respiratory_department_id = (SELECT id FROM department WHERE name = '呼吸内科');
SET @neurology_department_id = (SELECT id FROM department WHERE name = '神经内科');
SET @orthopedics_department_id = (SELECT id FROM department WHERE name = '骨科');
SET @dermatology_department_id = (SELECT id FROM department WHERE name = '皮肤科');
SET @ophthalmology_department_id = (SELECT id FROM department WHERE name = '眼科');

INSERT INTO patient (user_id, real_name, gender, birthday, id_card, medical_card_no, status)
VALUES
    (@patient_demo_user_id, '张小明', 1, '1995-05-12', NULL, 'DEMO-MC-001', 1),
    (@patient_wang_user_id, '王女士', 2, '1988-11-03', NULL, 'DEMO-MC-002', 1),
    (@patient_li_user_id, '李先生', 1, '1979-02-18', NULL, 'DEMO-MC-003', 1),
    (@patient_zhao_user_id, '赵女士', 2, '2001-08-26', NULL, 'DEMO-MC-004', 1),
    (@patient_chen_user_id, '陈晨', 2, '1993-01-15', NULL, 'DEMO-MC-005', 1),
    (@patient_zhou_user_id, '周然', 1, '1985-06-21', NULL, 'DEMO-MC-006', 1),
    (@patient_sun_user_id, '孙悦', 2, '1998-09-09', NULL, 'DEMO-MC-007', 1),
    (@patient_wu_user_id, '吴桐', 1, '1972-12-04', NULL, 'DEMO-MC-008', 1),
    (@patient_zheng_user_id, '郑宇', 1, '1990-03-27', NULL, 'DEMO-MC-009', 1),
    (@patient_feng_user_id, '冯雪', 2, '1982-07-19', NULL, 'DEMO-MC-010', 1),
    (@patient_he_user_id, '何嘉', 2, '2004-04-11', NULL, 'DEMO-MC-011', 1),
    (@patient_luo_user_id, '罗宁', 1, '1968-10-30', NULL, 'DEMO-MC-012', 1)
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    gender = VALUES(gender),
    status = VALUES(status);

SET @patient_demo_id = (SELECT id FROM patient WHERE user_id = @patient_demo_user_id);
SET @patient_wang_id = (SELECT id FROM patient WHERE user_id = @patient_wang_user_id);
SET @patient_li_id = (SELECT id FROM patient WHERE user_id = @patient_li_user_id);
SET @patient_zhao_id = (SELECT id FROM patient WHERE user_id = @patient_zhao_user_id);
SET @patient_chen_id = (SELECT id FROM patient WHERE user_id = @patient_chen_user_id);
SET @patient_zhou_id = (SELECT id FROM patient WHERE user_id = @patient_zhou_user_id);
SET @patient_sun_id = (SELECT id FROM patient WHERE user_id = @patient_sun_user_id);
SET @patient_wu_id = (SELECT id FROM patient WHERE user_id = @patient_wu_user_id);
SET @patient_zheng_id = (SELECT id FROM patient WHERE user_id = @patient_zheng_user_id);
SET @patient_feng_id = (SELECT id FROM patient WHERE user_id = @patient_feng_user_id);
SET @patient_he_id = (SELECT id FROM patient WHERE user_id = @patient_he_user_id);
SET @patient_luo_id = (SELECT id FROM patient WHERE user_id = @patient_luo_user_id);

INSERT INTO doctor (
    user_id, name, gender, department_id, doctor_code, title,
    introduction, avatar_url, sort_order, status
)
VALUES
    (@cardio_user_id, '林知远', 1, @cardio_department_id, 'DOC-CARDIO-001', '主任医师',
     '擅长高血压、冠心病等常见心血管疾病的门诊诊疗。', NULL, 10, 1),
    (@cardio_2_user_id, '周景川', 1, @cardio_department_id, 'DOC-CARDIO-002', '副主任医师',
     '擅长心律失常、心力衰竭及心血管慢病管理。', NULL, 20, 1),
    (@digestive_user_id, '陈书宁', 2, @digestive_department_id, 'DOC-DIGEST-001', '副主任医师',
     '擅长胃肠道常见疾病和慢性消化系统疾病管理。', NULL, 30, 1),
    (@digestive_2_user_id, '刘志强', 1, @digestive_department_id, 'DOC-DIGEST-002', '主治医师',
     '擅长胃食管反流、消化不良及肝胆疾病诊疗。', NULL, 40, 1),
    (@pediatrics_user_id, '苏念安', 2, @pediatrics_department_id, 'DOC-PEDS-001', '主治医师',
     '擅长儿童呼吸道及消化道常见疾病诊疗。', NULL, 50, 1),
    (@respiratory_user_id, '徐明哲', 1, @respiratory_department_id, 'DOC-RESP-001', '副主任医师',
     '擅长哮喘、慢阻肺及呼吸道感染诊疗。', NULL, 60, 1),
    (@neurology_user_id, '何雨桐', 2, @neurology_department_id, 'DOC-NEURO-001', '主治医师',
     '擅长头痛、眩晕及脑血管疾病随访管理。', NULL, 70, 1),
    (@orthopedics_user_id, '顾承安', 1, @orthopedics_department_id, 'DOC-ORTHO-001', '主任医师',
     '擅长骨关节疾病、脊柱疾病及运动损伤诊疗。', NULL, 80, 1),
    (@dermatology_user_id, '方可欣', 2, @dermatology_department_id, 'DOC-DERM-001', '主治医师',
     '擅长湿疹、痤疮及常见过敏性皮肤病诊疗。', NULL, 90, 1),
    (@ophthalmology_user_id, '沈清和', 1, @ophthalmology_department_id, 'DOC-OPHTH-001', '副主任医师',
     '擅长屈光问题、干眼及常见眼表疾病诊疗。', NULL, 100, 1)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    name = VALUES(name),
    department_id = VALUES(department_id),
    title = VALUES(title),
    introduction = VALUES(introduction),
    sort_order = VALUES(sort_order),
    status = VALUES(status);

SET @cardio_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-CARDIO-001');
SET @cardio_2_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-CARDIO-002');
SET @digestive_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-DIGEST-001');
SET @digestive_2_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-DIGEST-002');
SET @pediatrics_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-PEDS-001');
SET @respiratory_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-RESP-001');
SET @neurology_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-NEURO-001');
SET @orthopedics_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-ORTHO-001');
SET @dermatology_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-DERM-001');
SET @ophthalmology_doctor_id = (SELECT id FROM doctor WHERE doctor_code = 'DOC-OPHTH-001');

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
    (@digestive_doctor_id, @two_weeks, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 6, 6, 1, 'OPEN', 0),
    (@cardio_2_doctor_id, @today, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 6, 4, 3, 'OPEN', 0),
    (@digestive_2_doctor_id, @today, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 20, 7, 5, 3, 'OPEN', 0),
    (@respiratory_doctor_id, @today, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 15, 8, 5, 4, 'OPEN', 0),
    (@neurology_doctor_id, @today, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 20, 6, 4, 3, 'OPEN', 0),
    (@orthopedics_doctor_id, @tomorrow, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 20, 5, 2, 4, 'OPEN', 0),
    (@dermatology_doctor_id, @tomorrow, 'AFTERNOON', '下午门诊', '14:00:00', '17:00:00', 15, 4, 0, 5, 'OPEN', 0),
    (@ophthalmology_doctor_id, @day_after_tomorrow, 'MORNING', '上午门诊', '08:00:00', '12:00:00', 15, 6, 4, 3, 'OPEN', 0)
ON DUPLICATE KEY UPDATE
    session_type = VALUES(session_type),
    session_name = VALUES(session_name),
    status = VALUES(status);

-- Give every demo doctor a real, bookable morning slot on each of the next 14 days.
-- Existing slots keep their live capacity when this script is run again.
INSERT INTO doctor_schedule_slot (
    doctor_id, schedule_date, session_type, session_name, start_time, end_time,
    average_consultation_minutes, total_capacity, remaining_capacity,
    next_queue_number, status, version
)
SELECT
    demo_doctor.id,
    DATE_ADD(@today, INTERVAL demo_day.day_offset DAY),
    'MORNING', '上午门诊', '08:00:00', '12:00:00',
    20, 8, 8, 1, 'OPEN', 0
FROM doctor demo_doctor
CROSS JOIN (
    SELECT 0 AS day_offset UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
    UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11
    UNION ALL SELECT 12 UNION ALL SELECT 13
) demo_day
WHERE demo_doctor.doctor_code IN (
    'DOC-CARDIO-001', 'DOC-CARDIO-002', 'DOC-DIGEST-001', 'DOC-DIGEST-002',
    'DOC-PEDS-001', 'DOC-RESP-001', 'DOC-NEURO-001', 'DOC-ORTHO-001',
    'DOC-DERM-001', 'DOC-OPHTH-001'
)
ON DUPLICATE KEY UPDATE
    session_type = VALUES(session_type),
    session_name = VALUES(session_name);

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
SET @cardio_2_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @cardio_2_doctor_id AND schedule_date = @today
      AND start_time = '14:00:00' AND end_time = '17:00:00'
);
SET @digestive_2_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @digestive_2_doctor_id AND schedule_date = @today
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @respiratory_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @respiratory_doctor_id AND schedule_date = @today
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @neurology_today_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @neurology_doctor_id AND schedule_date = @today
      AND start_time = '14:00:00' AND end_time = '17:00:00'
);
SET @orthopedics_tomorrow_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @orthopedics_doctor_id AND schedule_date = @tomorrow
      AND start_time = '08:00:00' AND end_time = '12:00:00'
);
SET @dermatology_tomorrow_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @dermatology_doctor_id AND schedule_date = @tomorrow
      AND start_time = '14:00:00' AND end_time = '17:00:00'
);
SET @ophthalmology_future_slot = (
    SELECT id FROM doctor_schedule_slot
    WHERE doctor_id = @ophthalmology_doctor_id AND schedule_date = @day_after_tomorrow
      AND start_time = '08:00:00' AND end_time = '12:00:00'
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

-- Additional daily appointments. The date in each identifier makes a new import on
-- another day useful, while rerunning it on the same day remains idempotent.
INSERT INTO appointment (
    appointment_no, request_id, patient_id, doctor_id, schedule_slot_id,
    queue_number, schedule_date, start_time, end_time, estimated_arrival_time,
    status, cancelled_at, cancel_reason, created_at
)
VALUES
    (CONCAT('DEMO-APT-', @demo_date_key, '-101'), CONCAT('demo-request-', @demo_date_key, '-101'),
     @patient_chen_id, @cardio_2_doctor_id, @cardio_2_today_slot, 1, @today,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-102'), CONCAT('demo-request-', @demo_date_key, '-102'),
     @patient_zhou_id, @cardio_2_doctor_id, @cardio_2_today_slot, 2, @today,
     '14:00:00', '17:00:00', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-103'), CONCAT('demo-request-', @demo_date_key, '-103'),
     @patient_sun_id, @digestive_2_doctor_id, @digestive_2_today_slot, 1, @today,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-104'), CONCAT('demo-request-', @demo_date_key, '-104'),
     @patient_wu_id, @digestive_2_doctor_id, @digestive_2_today_slot, 2, @today,
     '08:00:00', '12:00:00', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-105'), CONCAT('demo-request-', @demo_date_key, '-105'),
     @patient_zheng_id, @respiratory_doctor_id, @respiratory_today_slot, 1, @today,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-106'), CONCAT('demo-request-', @demo_date_key, '-106'),
     @patient_feng_id, @respiratory_doctor_id, @respiratory_today_slot, 2, @today,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-107'), CONCAT('demo-request-', @demo_date_key, '-107'),
     @patient_he_id, @respiratory_doctor_id, @respiratory_today_slot, 3, @today,
     '08:00:00', '12:00:00', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-108'), CONCAT('demo-request-', @demo_date_key, '-108'),
     @patient_luo_id, @neurology_doctor_id, @neurology_today_slot, 1, @today,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-109'), CONCAT('demo-request-', @demo_date_key, '-109'),
     @patient_chen_id, @neurology_doctor_id, @neurology_today_slot, 2, @today,
     '14:00:00', '17:00:00', NULL, 'COMPLETED', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-110'), CONCAT('demo-request-', @demo_date_key, '-110'),
     @patient_zhou_id, @orthopedics_doctor_id, @orthopedics_tomorrow_slot, 1, @tomorrow,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-111'), CONCAT('demo-request-', @demo_date_key, '-111'),
     @patient_sun_id, @orthopedics_doctor_id, @orthopedics_tomorrow_slot, 2, @tomorrow,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-112'), CONCAT('demo-request-', @demo_date_key, '-112'),
     @patient_wu_id, @orthopedics_doctor_id, @orthopedics_tomorrow_slot, 3, @tomorrow,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-113'), CONCAT('demo-request-', @demo_date_key, '-113'),
     @patient_zheng_id, @dermatology_doctor_id, @dermatology_tomorrow_slot, 1, @tomorrow,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-114'), CONCAT('demo-request-', @demo_date_key, '-114'),
     @patient_feng_id, @dermatology_doctor_id, @dermatology_tomorrow_slot, 2, @tomorrow,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-115'), CONCAT('demo-request-', @demo_date_key, '-115'),
     @patient_he_id, @dermatology_doctor_id, @dermatology_tomorrow_slot, 3, @tomorrow,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-116'), CONCAT('demo-request-', @demo_date_key, '-116'),
     @patient_luo_id, @dermatology_doctor_id, @dermatology_tomorrow_slot, 4, @tomorrow,
     '14:00:00', '17:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-117'), CONCAT('demo-request-', @demo_date_key, '-117'),
     @patient_chen_id, @ophthalmology_doctor_id, @ophthalmology_future_slot, 1, @day_after_tomorrow,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-118'), CONCAT('demo-request-', @demo_date_key, '-118'),
     @patient_zhou_id, @ophthalmology_doctor_id, @ophthalmology_future_slot, 2, @day_after_tomorrow,
     '08:00:00', '12:00:00', NULL, 'BOOKED', NULL, NULL, NOW()),
    (CONCAT('DEMO-APT-', @demo_date_key, '-119'), CONCAT('demo-request-', @demo_date_key, '-119'),
     @patient_demo_id, @cardio_2_doctor_id, @cardio_2_today_slot, 3, @today,
     '14:00:00', '17:00:00', NULL, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 3 HOUR),
     '患者主动取消', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (CONCAT('DEMO-APT-', @demo_date_key, '-120'), CONCAT('demo-request-', @demo_date_key, '-120'),
     @patient_wang_id, @dermatology_doctor_id, @dermatology_tomorrow_slot, 5, @tomorrow,
     '14:00:00', '17:00:00', NULL, 'CANCELLED', DATE_SUB(NOW(), INTERVAL 1 HOUR),
     '行程冲突', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
    request_id = request_id;

INSERT INTO appointment_waitlist (
    patient_id, schedule_slot_id, status, offer_expire_time, created_at
)
VALUES
    (@patient_zhao_id, @digestive_tomorrow_slot, 'WAITING', NULL, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
    (@patient_wang_id, @digestive_tomorrow_slot, 'OFFERED', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
    (@patient_demo_id, @dermatology_tomorrow_slot, 'WAITING', NULL, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
    (@patient_li_id, @dermatology_tomorrow_slot, 'WAITING', NULL, DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
    (@patient_zhao_id, @dermatology_tomorrow_slot, 'OFFERED', DATE_ADD(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 25 MINUTE))
ON DUPLICATE KEY UPDATE
    patient_id = VALUES(patient_id);

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

INSERT INTO appointment_waitlist (
    patient_id, schedule_slot_id, status, offer_expire_time, created_at
)
SELECT @patient_chen_id, @dermatology_tomorrow_slot, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 10 MINUTE),
       DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (
    SELECT 1 FROM appointment_waitlist
    WHERE patient_id = @patient_chen_id
      AND schedule_slot_id = @dermatology_tomorrow_slot
      AND status = 'EXPIRED'
);

COMMIT;
