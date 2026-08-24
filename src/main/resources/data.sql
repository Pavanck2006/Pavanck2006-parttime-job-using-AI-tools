-- ==============================================================================
-- SAMPLE DATA: PartTime Job Platform
-- Note: Passwords below match BCrypt hashes for 'Admin@123', 'Owner@123', 'Student@123'
-- Hash for Admin@123: $2a$10$e76r4/Y8d7hTjXF5.hG2EOG8bZ4xsmL102c9Rnm5k70n.2Nn26d6e
-- (Spring Boot DataInitializer also checks and creates seeds programmatically)
-- ==============================================================================

-- 1. Insert Admin User
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (1, 'admin@parttimejob.com', '$2a$10$VjQ30o7JmN2Gz1jC0j3gV.6e9Z299mNqYhMekY360jM4fW16lq2q2', 'System Administrator', '+919876543210', 'ROLE_ADMIN', TRUE, FALSE);

-- 2. Insert Catering Owners
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (2, 'owner.srilakshmi@catering.com', '$2a$10$VjQ30o7JmN2Gz1jC0j3gV.6e9Z299mNqYhMekY360jM4fW16lq2q2', 'Ramesh Rao', '+919845012345', 'ROLE_OWNER', TRUE, FALSE);

INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (3, 'owner.royal@catering.com', '$2a$10$VjQ30o7JmN2Gz1jC0j3gV.6e9Z299mNqYhMekY360jM4fW16lq2q2', 'Suresh Kumar', '+919845098765', 'ROLE_OWNER', TRUE, FALSE);

-- 3. Insert Students
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (4, 'student.pavan@gmail.com', '$2a$10$VjQ30o7JmN2Gz1jC0j3gV.6e9Z299mNqYhMekY360jM4fW16lq2q2', 'Pavan Kumar', '+919900112233', 'ROLE_STUDENT', TRUE, FALSE);

INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (5, 'student.ananya@gmail.com', '$2a$10$VjQ30o7JmN2Gz1jC0j3gV.6e9Z299mNqYhMekY360jM4fW16lq2q2', 'Ananya Sharma', '+919900445566', 'ROLE_STUDENT', TRUE, FALSE);

-- Profiles
INSERT IGNORE INTO owner_profiles (id, user_id, catering_name, business_address, business_phone, verification_status, verified_at, total_jobs_posted)
VALUES (1, 2, 'Sri Lakshmi Catering Services', '124, 8th Main, Kengeri Satellite Town, Bangalore', '+919845012345', 'VERIFIED', NOW(), 3);

INSERT IGNORE INTO owner_profiles (id, user_id, catering_name, business_address, business_phone, verification_status, verified_at, total_jobs_posted)
VALUES (2, 3, 'Royal Grand Events & Catering', '55, 100ft Road, Indiranagar, Bangalore', '+919845098765', 'PENDING_VERIFICATION', NULL, 1);

INSERT IGNORE INTO student_profiles (id, user_id, college_name, preferred_area, skills, bio, emergency_contact, total_jobs_completed, rating)
VALUES (1, 4, 'Bangalore Institute of Technology', 'Kengeri', 'Food Serving, Buffet Management, Table Setup', 'Hardworking 3rd year engineering student looking for weekend catering jobs.', '+919900000001', 5, 4.9);

INSERT IGNORE INTO student_profiles (id, user_id, college_name, preferred_area, skills, bio, emergency_contact, total_jobs_completed, rating)
VALUES (2, 5, 'R.V. College of Engineering', 'Koramangala', 'Guest Hospitality, Counter Assistance, Cleaner', 'Punctual student with prior banquet server experience.', '+919900000002', 3, 4.8);
