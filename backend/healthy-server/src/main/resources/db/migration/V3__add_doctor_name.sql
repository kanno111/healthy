-- 医生档案需要姓名，供管理端和患者端展示。
-- 已执行的 V2 不再修改，后续结构变化通过新迁移追加。
ALTER TABLE doctor
    ADD COLUMN name VARCHAR(50) NOT NULL COMMENT '医生姓名' AFTER id;
