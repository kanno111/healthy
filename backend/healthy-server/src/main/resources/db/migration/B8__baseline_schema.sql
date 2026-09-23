-- Current schema baseline captured from the MySQL 8.0.34 development database.
--
-- Existing databases are baselined at version 8 and MUST NOT execute this file.
-- Empty databases use this baseline migration to create the complete schema.
-- Business/demo data is intentionally excluded.

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='system users';

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
    CONSTRAINT chk_patient_gender CHECK (gender IN (1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='patients';

CREATE TABLE doctor (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'logical reference to the doctor login account',
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

CREATE TABLE doctor_schedule_slot (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    doctor_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to doctor.id',
    schedule_date DATE NOT NULL COMMENT 'schedule date',
    session_type VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'MORNING, AFTERNOON, OTHER',
    -- Preserve the version-8 database default exactly; change it later only through a new V migration.
    session_name VARCHAR(20) NOT NULL DEFAULT '闂ㄨ瘖' COMMENT 'session display name',
    start_time TIME NOT NULL COMMENT 'start time',
    end_time TIME NOT NULL COMMENT 'end time',
    average_consultation_minutes INT UNSIGNED NOT NULL DEFAULT 10 COMMENT 'average consultation minutes',
    total_capacity INT UNSIGNED NOT NULL COMMENT 'total capacity',
    remaining_capacity INT UNSIGNED NOT NULL COMMENT 'remaining capacity',
    next_queue_number INT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'next queue number',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, CLOSED',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot_doctor_date_time (doctor_id, schedule_date, start_time, end_time),
    KEY idx_slot_date_status (schedule_date, status),
    KEY idx_slot_doctor_date (doctor_id, schedule_date),
    CONSTRAINT chk_slot_time CHECK (start_time < end_time),
    CONSTRAINT chk_slot_capacity CHECK (remaining_capacity <= total_capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='doctor schedule slots and stock';

CREATE TABLE appointment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    appointment_no VARCHAR(32) NOT NULL COMMENT 'appointment number',
    request_id VARCHAR(64) NOT NULL COMMENT 'idempotency request identifier',
    patient_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to patient.id',
    doctor_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to doctor.id',
    schedule_slot_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to doctor_schedule_slot.id',
    queue_number INT UNSIGNED DEFAULT NULL COMMENT 'queue number in the schedule slot',
    schedule_date DATE NOT NULL COMMENT 'appointment date snapshot',
    start_time TIME NOT NULL COMMENT 'appointment start time snapshot',
    end_time TIME NOT NULL COMMENT 'appointment end time snapshot',
    estimated_arrival_time DATETIME DEFAULT NULL COMMENT 'estimated arrival time',
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED, CANCELLED, COMPLETED, NO_SHOW',
    cancelled_at DATETIME DEFAULT NULL COMMENT 'cancelled time',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT 'cancellation reason',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    active_booking_key VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'BOOKED' THEN CONCAT(patient_id, ':', schedule_slot_id) ELSE NULL END
        ) STORED COMMENT 'unique key for an active appointment',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    UNIQUE KEY uk_appointment_request_id (request_id),
    UNIQUE KEY uk_appointment_active_booking (active_booking_key),
    KEY idx_appointment_patient_created (patient_id, created_at),
    KEY idx_appointment_doctor_date (doctor_id, schedule_date),
    KEY idx_appointment_slot_status (schedule_slot_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='appointments';

CREATE TABLE appointment_waitlist (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    patient_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to patient.id',
    schedule_slot_id BIGINT UNSIGNED NOT NULL COMMENT 'logical reference to doctor_schedule_slot.id',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING, OFFERED, CONFIRMED, EXPIRED, CANCELLED',
    offer_expire_time DATETIME DEFAULT NULL COMMENT 'offer confirmation deadline',
    active_waitlist_key VARCHAR(80)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('WAITING', 'OFFERED') THEN CONCAT(patient_id, ':', schedule_slot_id)
                ELSE NULL
            END
        ) STORED COMMENT 'unique key for an active waitlist entry',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_waitlist_active (active_waitlist_key),
    KEY idx_waitlist_slot_status_created (schedule_slot_id, status, created_at, id),
    KEY idx_waitlist_patient_created (patient_id, created_at),
    CONSTRAINT chk_waitlist_status CHECK (status IN ('WAITING', 'OFFERED', 'CONFIRMED', 'EXPIRED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='appointment waitlist';
