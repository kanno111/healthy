-- identity-service owns this schema. References from other services are logical IDs.

CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    username VARCHAR(50) NOT NULL COMMENT 'login username',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    name VARCHAR(50) NOT NULL COMMENT 'display name',
    phone VARCHAR(20) NOT NULL COMMENT 'phone number',
    role VARCHAR(20) NOT NULL COMMENT 'PATIENT, STAFF, DOCTOR',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_role_status (role, status),
    CONSTRAINT chk_sys_user_role CHECK (role IN ('PATIENT', 'STAFF', 'DOCTOR')),
    CONSTRAINT chk_sys_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='system users';

CREATE TABLE patient (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to sys_user.id',
    real_name VARCHAR(50) NOT NULL COMMENT 'patient real name',
    gender TINYINT NOT NULL COMMENT '1 male, 2 female',
    birthday DATE DEFAULT NULL COMMENT 'birthday',
    id_card VARCHAR(18) DEFAULT NULL COMMENT 'identity card number',
    medical_card_no VARCHAR(32) DEFAULT NULL COMMENT 'medical card number',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_user_id (user_id),
    UNIQUE KEY uk_patient_id_card (id_card),
    UNIQUE KEY uk_patient_medical_card_no (medical_card_no),
    CONSTRAINT chk_patient_gender CHECK (gender IN (1, 2)),
    CONSTRAINT chk_patient_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='patients';
