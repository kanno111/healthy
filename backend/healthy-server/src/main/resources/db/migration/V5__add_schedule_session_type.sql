-- 固定上午、下午两类班次，仅 OTHER 类型允许使用自定义名称。
ALTER TABLE doctor_schedule_slot
    ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT '班次类型：MORNING、AFTERNOON、OTHER' AFTER schedule_date;

UPDATE doctor_schedule_slot SET session_type = 'MORNING' WHERE session_name = '上午门诊';
UPDATE doctor_schedule_slot SET session_type = 'AFTERNOON' WHERE session_name = '下午门诊';
