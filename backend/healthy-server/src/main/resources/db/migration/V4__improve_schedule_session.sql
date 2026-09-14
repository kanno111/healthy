-- 将号源模型明确为上午/下午等出诊班次，并为预约排队预留必要字段。
ALTER TABLE doctor_schedule_slot
    ADD COLUMN session_name VARCHAR(20) NOT NULL DEFAULT '门诊' COMMENT '班次名称，例如上午门诊' AFTER schedule_date,
    ADD COLUMN average_consultation_minutes INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '平均每位患者接诊分钟数' AFTER end_time,
    ADD COLUMN next_queue_number INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '下一个可分配的排队号' AFTER remaining_capacity;

ALTER TABLE appointment
    ADD COLUMN queue_number INT UNSIGNED DEFAULT NULL COMMENT '患者在当前班次中的排队号' AFTER schedule_slot_id,
    ADD COLUMN estimated_arrival_time DATETIME DEFAULT NULL COMMENT '预约成功时计算并保存的预计到诊时间' AFTER end_time;
