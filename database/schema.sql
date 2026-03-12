-- ============================================================
-- AI-Powered Smart Health Diagnosis and Appointment System
-- MySQL Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_health_db;
USE smart_health_db;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- BCrypt hashed
    phone       VARCHAR(20),
    role        ENUM('PATIENT','DOCTOR','ADMIN') NOT NULL DEFAULT 'PATIENT',
    gender      ENUM('MALE','FEMALE','OTHER'),
    date_of_birth DATE,
    address     TEXT,
    profile_pic VARCHAR(255),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- DOCTORS TABLE
-- ============================================================
CREATE TABLE doctors (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT NOT NULL UNIQUE,
    specialization     VARCHAR(100) NOT NULL,
    license_number     VARCHAR(50) UNIQUE,
    years_experience   INT DEFAULT 0,
    hospital_name      VARCHAR(150),
    consultation_fee   DECIMAL(10,2) DEFAULT 0.00,
    bio                TEXT,
    available_days     VARCHAR(100),   -- e.g. "MON,TUE,WED"
    slot_start_time    TIME,
    slot_end_time      TIME,
    slot_duration_min  INT DEFAULT 30,
    rating             DECIMAL(3,2) DEFAULT 0.00,
    is_verified        BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- SYMPTOMS TABLE  (master list of known symptoms)
-- ============================================================
CREATE TABLE symptoms (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    body_part   VARCHAR(50),
    severity    ENUM('MILD','MODERATE','SEVERE') DEFAULT 'MILD',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- PREDICTIONS TABLE
-- ============================================================
CREATE TABLE predictions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id           BIGINT NOT NULL,
    symptom_ids          TEXT NOT NULL,          -- JSON array of symptom IDs
    symptom_names        TEXT NOT NULL,          -- comma-separated names
    predicted_disease    VARCHAR(150) NOT NULL,
    confidence_score     DECIMAL(5,4),           -- 0.0000 to 1.0000
    alternative_diseases TEXT,                   -- JSON array
    recommended_specialty VARCHAR(100),
    ai_model_version     VARCHAR(20) DEFAULT '1.0',
    notes                TEXT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- APPOINTMENTS TABLE
-- ============================================================
CREATE TABLE appointments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id      BIGINT NOT NULL,
    doctor_id       BIGINT NOT NULL,
    prediction_id   BIGINT,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_min    INT DEFAULT 30,
    status          ENUM('PENDING','APPROVED','REJECTED','COMPLETED','CANCELLED')
                    DEFAULT 'PENDING',
    reason          TEXT,
    doctor_notes    TEXT,
    rejection_reason TEXT,
    meeting_link    VARCHAR(255),    -- for telemedicine
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)  REFERENCES users(id)        ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)   REFERENCES doctors(id)      ON DELETE CASCADE,
    FOREIGN KEY (prediction_id) REFERENCES predictions(id) ON DELETE SET NULL
);

-- ============================================================
-- MEDICAL HISTORY TABLE
-- ============================================================
CREATE TABLE medical_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id     BIGINT NOT NULL,
    doctor_id      BIGINT,
    appointment_id BIGINT,
    diagnosis      VARCHAR(200),
    prescription   TEXT,
    lab_results    TEXT,
    allergies      TEXT,
    chronic_conditions TEXT,
    notes          TEXT,
    visit_date     DATE NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)     REFERENCES users(id)        ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)      REFERENCES doctors(id)      ON DELETE SET NULL,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

-- ============================================================
-- DOCTOR TIME SLOTS TABLE
-- ============================================================
CREATE TABLE doctor_slots (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id   BIGINT NOT NULL,
    slot_date   DATE NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    is_booked   BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_slot (doctor_id, slot_date, start_time),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_role        ON users(role);
CREATE INDEX idx_doctors_specialty ON doctors(specialization);
CREATE INDEX idx_appt_patient      ON appointments(patient_id);
CREATE INDEX idx_appt_doctor       ON appointments(doctor_id);
CREATE INDEX idx_appt_status       ON appointments(status);
CREATE INDEX idx_pred_patient      ON predictions(patient_id);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Admin user (password: Admin@123)
INSERT INTO users (full_name, email, password, role, is_active) VALUES
('System Admin', 'admin@smarthealth.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh', 'ADMIN', TRUE);

-- Seed symptoms
INSERT INTO symptoms (name, description, body_part, severity) VALUES
('Fever',            'Elevated body temperature above 37.5°C',   'Whole body',  'MODERATE'),
('Cough',            'Persistent cough, dry or productive',       'Respiratory', 'MILD'),
('Headache',         'Pain in the head or upper neck',            'Head',        'MILD'),
('Fatigue',          'Unusual tiredness or lack of energy',       'Whole body',  'MILD'),
('Shortness of Breath','Difficulty breathing or breathlessness',  'Respiratory', 'SEVERE'),
('Chest Pain',       'Pain, pressure or discomfort in chest',     'Chest',       'SEVERE'),
('Nausea',           'Feeling of sickness with urge to vomit',    'Abdomen',     'MILD'),
('Vomiting',         'Forceful expulsion of stomach contents',    'Abdomen',     'MODERATE'),
('Diarrhea',         'Loose, watery stools three or more times',  'Abdomen',     'MODERATE'),
('Stomach Pain',     'Pain or discomfort in the abdominal area',  'Abdomen',     'MODERATE'),
('Sore Throat',      'Pain or irritation in the throat',          'Throat',      'MILD'),
('Runny Nose',       'Excess nasal discharge',                    'Nose',        'MILD'),
('Body Aches',       'General muscle pain and aches',             'Whole body',  'MILD'),
('Chills',           'Feeling cold with shivering',               'Whole body',  'MILD'),
('Rash',             'Skin eruption or change in skin texture',   'Skin',        'MILD'),
('Joint Pain',       'Pain in one or more joints',                'Joints',      'MODERATE'),
('Dizziness',        'Feeling faint, woozy or unsteady',          'Head',        'MODERATE'),
('Swollen Lymph Nodes','Enlarged lymph nodes',                    'Neck',        'MODERATE'),
('Loss of Appetite', 'Reduced desire to eat',                     'Abdomen',     'MILD'),
('Night Sweats',     'Heavy sweating during sleep',               'Whole body',  'MODERATE'),
('Blurred Vision',   'Reduced clarity of vision',                 'Eyes',        'MODERATE'),
('Frequent Urination','Urge to urinate more often than usual',    'Urinary',     'MILD'),
('Excessive Thirst', 'Feeling unusually or extremely thirsty',    'Whole body',  'MILD'),
('Weight Loss',      'Unexplained decrease in body weight',       'Whole body',  'MODERATE'),
('Itching',          'Irritating skin sensation causing scratching','Skin',      'MILD'),
('Swelling',         'Abnormal enlargement of body part',         'Various',     'MODERATE'),
('Back Pain',        'Pain in the lower or upper back',           'Back',        'MODERATE'),
('Confusion',        'Disorientation or difficulty thinking',     'Head',        'SEVERE'),
('Palpitations',     'Noticeably rapid or irregular heartbeat',   'Chest',       'MODERATE'),
('Sneezing',         'Involuntary expulsion of air from nose',    'Nose',        'MILD');
