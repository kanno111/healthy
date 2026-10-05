-- doctor-service owns this schema. Cross-context references, such as user_id,
-- are logical IDs only and intentionally have no database foreign key.

CREATE TABLE department (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    name VARCHAR(50) NOT NULL COMMENT 'department name',
    description VARCHAR(500) DEFAULT NULL COMMENT 'department description',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'ascending display order',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_name (name),
    KEY idx_department_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='departments';

CREATE TABLE doctor (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'logical reference to identity user ID',
    name VARCHAR(50) NOT NULL COMMENT 'doctor name',
    gender TINYINT NOT NULL COMMENT '1 male, 2 female',
    department_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to department.id',
    doctor_code VARCHAR(32) NOT NULL COMMENT 'doctor code',
    title VARCHAR(50) DEFAULT NULL COMMENT 'professional title',
    introduction TEXT DEFAULT NULL COMMENT 'doctor introduction and specialties',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT 'avatar URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'ascending display order',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_code (doctor_code),
    UNIQUE KEY uk_doctor_user_id (user_id),
    KEY idx_doctor_department_status_sort (department_id, status, sort_order),
    CONSTRAINT chk_doctor_gender CHECK (gender IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='doctors';
