
DROP TABLE IF EXISTS reschedule_history;
-- waitlist_preferred_dates references waitlist
DROP TABLE IF EXISTS waitlist_preferred_dates;
-- slot_cancellations references appointments and doctors
DROP TABLE IF EXISTS slot_cancellations;
-- notifications references appointments, patients, and doctors
DROP TABLE IF EXISTS notifications;
-- waitlist references patients and doctors
DROP TABLE IF EXISTS waitlist;
-- appointments references patients and doctors
DROP TABLE IF EXISTS appointments;
-- No tables reference patients
DROP TABLE IF EXISTS patients;
-- No tables reference doctors
DROP TABLE IF EXISTS doctors;


-- ***********************************
-- 2. CREATE TABLES (Original Order is Fine)
-- ***********************************

-- Doctors table
CREATE TABLE doctors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    zoho_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    time_zone VARCHAR(50),
    specialization VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Patients table
CREATE TABLE patients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vip BOOLEAN NOT NULL DEFAULT FALSE,
    severity_level INTEGER NOT NULL DEFAULT 1,
    visit_count INTEGER NOT NULL DEFAULT 0,
    total_notifications_sent INTEGER NOT NULL DEFAULT 0,
    total_notifications_responded INTEGER NOT NULL DEFAULT 0,
    consecutive_misses INTEGER NOT NULL DEFAULT 0,
    inactive_until DATETIME,
    last_notified_at DATETIME,
    staff_notes TEXT
);

-- Appointments table
CREATE TABLE appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    zoho_id VARCHAR(50) NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status ENUM('upcoming','cancelled','completed','rescheduled') DEFAULT 'upcoming',
    source ENUM('zoho','manual') DEFAULT 'zoho',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_whatsapp_number BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Waitlist table
CREATE TABLE waitlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- New table for multiple preferred dates
CREATE TABLE waitlist_preferred_dates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    waitlist_id BIGINT NOT NULL,
    preferred_date DATE NOT NULL,
    -- NOTE: ON DELETE CASCADE is a good practice here.
    FOREIGN KEY (waitlist_id) REFERENCES waitlist(id) ON DELETE CASCADE
);


-- Slot Cancellations table
CREATE TABLE slot_cancellations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    cancelled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_sent BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

-- Notifications table
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    doctor_id BIGINT NOT NULL,
    notification_type ENUM('slot_open','reminder','confirmation') NOT NULL,
    status ENUM('pending','sent','responded','expired') DEFAULT 'pending',
    sent_at TIMESTAMP,
    response ENUM('YES','NO'),
    expires_at TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

-- Reschedule History table
CREATE TABLE reschedule_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    old_appointment_id BIGINT NOT NULL,
    new_appointment_id BIGINT NOT NULL,
    rescheduled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (old_appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (new_appointment_id) REFERENCES appointments(id)
);