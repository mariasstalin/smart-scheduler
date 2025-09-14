-- Sample Smart Scheduler test data

-- Users
INSERT INTO users (id, name, email, password) VALUES
(1, 'Alice Johnson', 'alice@example.com', '$2a$10$123456789012345678901uZlQJp'), -- bcrypt dummy hash
(2, 'Bob Smith', 'bob@example.com', '$2a$10$987654321098765432109u8JkLm');

-- Appointments
INSERT INTO appointments (id, title, time, user_id) VALUES
(1, 'Dentist Appointment', '2025-09-20 10:00:00', 1),
(2, 'Team Meeting', '2025-09-21 14:00:00', 2);

-- Notifications
INSERT INTO notifications (id, recipient, message, status) VALUES
(1, '+1234567890', 'Reminder: Dentist Appointment', 'SENT'),
(2, '+1987654321', 'Reminder: Team Meeting', 'QUEUED');


-- Default Admin User
INSERT INTO users (id, name, email, password, role) VALUES
(100, 'Admin User', 'admin@example.com', '$2a$10$ZSPX3WwHjE7F2QqV9jS7Uu1iWj6X3T0jM6Fl1vUHD9IzfF5FzT92K', 'ADMIN');
-- password = admin123


-- Sample Appointment for Admin User
INSERT INTO appointments (id, user_id, title, description, start_time, end_time, status)
VALUES (200, 100, 'Hackathon Demo Meeting', 'Kickoff meeting for Smart Scheduler demo',
        '2025-09-15 10:00:00', '2025-09-15 11:00:00', 'CONFIRMED');


-- Sample WhatsApp Notification (Mock via Twilio)
INSERT INTO notifications (id, user_id, appointment_id, channel, message, status, created_at)
VALUES (300, 100, 200, 'WHATSAPP', 'Reminder: Hackathon Demo Meeting at 10:00 AM',
        'SENT', NOW());
