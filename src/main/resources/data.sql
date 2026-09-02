-- =============================================================================
-- SEED DATA: PartTime Job Platform
-- Only the admin account is seeded (admin cannot self-register).
-- All other accounts and jobs are created by users through the app.
-- =============================================================================

-- Admin account (password: Admin@123)
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role)
VALUES (1, 'admin@parttimejob.com', '$2a$10$9CarXmRbD3mKT7UJjSPSo.HJkgeFYqgNvqhAUxm5Zlp2u6ARlq61e', 'Platform Admin', '9999999999', 'ROLE_ADMIN');

-- Sample verified owner (password: Owner@123)
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role)
VALUES (2, 'owner.srilakshmi@catering.com', '$2a$10$e4wlcMo/hHmpHGStKvAA2eUyfR/0fICb2vilmwRqz3oQMaFEwNs9O', 'Sri Lakshmi', '9876543210', 'ROLE_OWNER');

INSERT IGNORE INTO owner_profiles (user_id, catering_name, business_address, business_phone, verification_status)
VALUES (2, 'Sri Lakshmi Catering Services', 'Koramangala, Bangalore', '9876543210', 'VERIFIED');

-- Sample student (password: Student@123)
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role)
VALUES (3, 'student.pavan@gmail.com', '$2a$10$ZKJ2LAWgW3bXkAQHAv/cp.M6BjmAdmJtGXpNLw5V/iOY58RW7/HbG', 'Pavan Kumar', '9123456789', 'ROLE_STUDENT');

INSERT IGNORE INTO student_profiles (user_id, college_name, preferred_area, skills, bio, emergency_contact)
VALUES (3, 'R.V. College of Engineering', 'Koramangala', 'Guest Hospitality, Counter Assistance', 'Punctual student with prior catering experience.', '9999999999');
