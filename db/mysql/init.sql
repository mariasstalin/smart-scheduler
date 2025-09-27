-- =====================================================
-- Smart Scheduler Database Initialization
-- =====================================================

-- Drop existing tables (for clean re-run during dev)
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS users;

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER'
);

-- =====================================================
-- Appointments Table
-- =====================================================
CREATE TABLE appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================================================
-- Notifications Table
-- =====================================================
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    appointment_id BIGINT,
    recipient VARCHAR(150),
    channel VARCHAR(50),
    message TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

-- =====================================================
-- Sample Data
-- =====================================================

-- Users
INSERT INTO users (id, name, email, password, role) VALUES
(1, 'Alice Johnson', 'alice@example.com', '$2a$10$123456789012345678901uZlQJp', 'USER'),
(2, 'Bob Smith', 'bob@example.com', '$2a$10$987654321098765432109u8JkLm', 'USER'),
(100, 'Admin User', 'admin@example.com', '$2a$10$ZSPX3WwHjE7F2QqV9jS7Uu1iWj6X3T0jM6Fl1vUHD9IzfF5FzT92K', 'ADMIN');
-- password = admin123

-- Appointments
INSERT INTO appointments (id, user_id, title, description, start_time, end_time, status) VALUES
(1, 1, 'Dentist Appointment', 'Routine checkup',
 '2025-09-20 10:00:00', '2025-09-20 11:00:00', 'CONFIRMED'),
(2, 2, 'Team Meeting', 'Project discussion',
 '2025-09-21 14:00:00', '2025-09-21 15:00:00', 'PENDING'),
(200, 100, 'Hackathon Demo Meeting', 'Kickoff meeting for Smart Scheduler demo',
 '2025-09-15 10:00:00', '2025-09-15 11:00:00', 'CONFIRMED');

-- Notifications
INSERT INTO notifications (id, user_id, appointment_id, recipient, channel, message, status, delivered) VALUES
(1, 1, 1, '+1234567890', 'EMAIL', 'Reminder: Dentist Appointment', 'SENT', TRUE),
(2, 2, 2, '+1987654321', 'EMAIL', 'Reminder: Team Meeting', 'QUEUED', FALSE),
(300, 100, 200, '+14155238886', 'WHATSAPP', 'Reminder: Hackathon Demo Meeting at 10:00 AM', 'SENT', TRUE);
