-- 预约挂号核心领域表。
-- 本项目使用逻辑外键：字段以 *_id 命名并建立索引，关联完整性由业务层事务保证。

CREATE TABLE department (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(50) NOT NULL COMMENT '科室名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '科室介绍',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_name (name),
    KEY idx_department_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

CREATE TABLE patient (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 sys_user.id',
    real_name VARCHAR(50) NOT NULL COMMENT '就诊人真实姓名',
    gender TINYINT NOT NULL COMMENT '性别：1-男，2-女',
    birthday DATE DEFAULT NULL COMMENT '出生日期',
    id_card VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    medical_card_no VARCHAR(32) DEFAULT NULL COMMENT '就诊卡号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_user_id (user_id),
    UNIQUE KEY uk_patient_id_card (id_card),
    UNIQUE KEY uk_patient_medical_card_no (medical_card_no),
    CONSTRAINT chk_patient_gender CHECK (gender IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者档案表';

CREATE TABLE doctor (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    gender TINYINT NOT NULL COMMENT '性别：1-男，2-女',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 department.id',
    doctor_code VARCHAR(32) NOT NULL COMMENT '医生工号',
    title VARCHAR(50) DEFAULT NULL COMMENT '职称，例如：主任医师',
    introduction TEXT DEFAULT NULL COMMENT '医生简介与擅长领域',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-可出诊，0-停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_code (doctor_code),
    KEY idx_doctor_department_status_sort (department_id, status, sort_order),
    CONSTRAINT chk_doctor_gender CHECK (gender IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生档案表';

CREATE TABLE doctor_schedule_slot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    doctor_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 doctor.id',
    schedule_date DATE NOT NULL COMMENT '出诊日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    total_capacity INT UNSIGNED NOT NULL COMMENT '总号源数',
    remaining_capacity INT UNSIGNED NOT NULL COMMENT '剩余号源数',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN-可预约，CLOSED-关闭',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot_doctor_date_time (doctor_id, schedule_date, start_time, end_time),
    KEY idx_slot_date_status (schedule_date, status),
    KEY idx_slot_doctor_date (doctor_id, schedule_date),
    CONSTRAINT chk_slot_time CHECK (start_time < end_time),
    CONSTRAINT chk_slot_capacity CHECK (remaining_capacity <= total_capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生排班时段与号源表';

CREATE TABLE appointment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    appointment_no VARCHAR(32) NOT NULL COMMENT '预约单号',
    patient_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 patient.id',
    doctor_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 doctor.id，创建时须与号源所属医生一致',
    schedule_slot_id BIGINT UNSIGNED NOT NULL COMMENT '逻辑关联 doctor_schedule_slot.id',
    schedule_date DATE NOT NULL COMMENT '预约日期快照',
    start_time TIME NOT NULL COMMENT '预约开始时间快照',
    end_time TIME NOT NULL COMMENT '预约结束时间快照',
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED' COMMENT '状态：BOOKED-已预约，CANCELLED-已取消，COMPLETED-已完成，NO_SHOW-爽约',
    cancelled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_appointment_patient_created (patient_id, created_at),
    KEY idx_appointment_doctor_date (doctor_id, schedule_date),
    KEY idx_appointment_slot_status (schedule_slot_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';
