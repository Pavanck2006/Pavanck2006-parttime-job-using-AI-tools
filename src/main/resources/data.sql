-- ==============================================================================
-- SAMPLE DATA: PartTime Job Platform
-- Note: Passwords below match BCrypt hashes for 'Admin@123', 'Owner@123', 'Student@123'
-- Hashes verified: Admin@123, Owner@123, Student@123
-- (Spring Boot DataInitializer also checks and creates seeds programmatically)
-- ==============================================================================

-- 1. Insert Admin User
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (1, 'admin@parttimejob.com', '$2a$10$F2EEUqTZOC6eGBLn9ridOeACEZSpw/zWzzgaACxGV5vGu20MXKrjS', 'System Administrator', '+919876543210', 'ROLE_ADMIN', TRUE, FALSE);

-- 2. Insert Catering Owners
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (2, 'owner.srilakshmi@catering.com', '$2a$10$XceQdTl8rxLDNrAwpLEI.eafjm083/F/Yiw434P191de7JEtovOLC', 'Ramesh Rao', '+919845012345', 'ROLE_OWNER', TRUE, FALSE);

INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (3, 'owner.royal@catering.com', '$2a$10$XceQdTl8rxLDNrAwpLEI.eafjm083/F/Yiw434P191de7JEtovOLC', 'Suresh Kumar', '+919845098765', 'ROLE_OWNER', TRUE, FALSE);

-- 3. Insert Students
INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (4, 'student.pavan@gmail.com', '$2a$10$z.lwekNRr4nQO8H2EQgBa.ZxBWvImK1k/2BfrKnOzXTnbKBRbzqvO', 'Pavan Kumar', '+919900112233', 'ROLE_STUDENT', TRUE, FALSE);

INSERT IGNORE INTO users (id, email, password_hash, full_name, phone, role, is_active, is_suspended)
VALUES (5, 'student.ananya@gmail.com', '$2a$10$z.lwekNRr4nQO8H2EQgBa.ZxBWvImK1k/2BfrKnOzXTnbKBRbzqvO', 'Ananya Sharma', '+919900445566', 'ROLE_STUDENT', TRUE, FALSE);

-- Profiles
INSERT IGNORE INTO owner_profiles (id, user_id, catering_name, business_address, business_phone, verification_status, verified_at, total_jobs_posted)
VALUES (1, 2, 'Sri Lakshmi Catering Services', '124, 8th Main, Kengeri Satellite Town, Bangalore', '+919845012345', 'VERIFIED', NOW(), 3);

INSERT IGNORE INTO owner_profiles (id, user_id, catering_name, business_address, business_phone, verification_status, verified_at, total_jobs_posted)
VALUES (2, 3, 'Royal Grand Events & Catering', '55, 100ft Road, Indiranagar, Bangalore', '+919845098765', 'PENDING_VERIFICATION', NULL, 1);

INSERT IGNORE INTO student_profiles (id, user_id, college_name, preferred_area, skills, bio, emergency_contact, total_jobs_completed, rating)
VALUES (1, 4, 'Bangalore Institute of Technology', 'Kengeri', 'Food Serving, Buffet Management, Table Setup', 'Hardworking 3rd year engineering student looking for weekend catering jobs.', '+919900000001', 5, 4.9);

INSERT IGNORE INTO student_profiles (id, user_id, college_name, preferred_area, skills, bio, emergency_contact, total_jobs_completed, rating)
VALUES (2, 5, 'R.V. College of Engineering', 'Koramangala', 'Guest Hospitality, Counter Assistance, Cleaner', 'Punctual student with prior banquet server experience.', '+919900000002', 3, 4.8);

-- 4. Seed Catering Jobs (from verified owner)
INSERT IGNORE INTO catering_jobs (id, owner_id, title, description, work_type, work_area, detailed_location, job_date, start_time, end_time, payment_amount, payment_type, is_on_spot_payment, workers_required, workers_selected, required_skills, contact_phone, contact_email, status)
VALUES (1, 1, 'Evening Wedding Buffet - Food Servers Needed', 'Serving guests at a wedding buffet. Black formal attire required. Meals provided during shift.', 'FOOD_SERVER', 'Kengeri', 'Sri Lakshmi Kalyana Mantapa, 8th Main, Kengeri Satellite Town, Bangalore', date('now', '+1 day'), '17:00', '23:00', 850.00, 'ON_SPOT_PAYMENT', 1, 6, 0, 'Food Serving, Formal Attire', '+919845012345', 'owner.srilakshmi@catering.com', 'OPEN');

INSERT IGNORE INTO catering_jobs (id, owner_id, title, description, work_type, work_area, detailed_location, job_date, start_time, end_time, payment_amount, payment_type, is_on_spot_payment, workers_required, workers_selected, required_skills, contact_phone, contact_email, status)
VALUES (2, 1, 'Corporate Lunch Event - Kitchen Helpers', 'Helping kitchen staff prep and plate food for a 200-person corporate lunch. Comfortable clothes and shoes required.', 'KITCHEN_HELPER', 'Indiranagar', 'The Grand Ballroom, 45, 100ft Road, Indiranagar, Bangalore', date('now', '+2 days'), '10:00', '15:00', 750.00, 'ON_SPOT_PAYMENT', 1, 4, 0, 'Kitchen Prep, Dish Handling', '+919845012345', 'owner.srilakshmi@catering.com', 'OPEN');

INSERT IGNORE INTO catering_jobs (id, owner_id, title, description, work_type, work_area, detailed_location, job_date, start_time, end_time, payment_amount, payment_type, is_on_spot_payment, workers_required, workers_selected, required_skills, contact_phone, contact_email, status)
VALUES (3, 1, 'Saturday Night DJ Party - Event Helpers', 'Helping set up and manage a private birthday party. Light lifting and guest assistance required.', 'EVENT_HELPER', 'Koramangala', 'Sunset Banquet Hall, 12th Main, Koramangala 5th Block, Bangalore', date('now', '+3 days'), '18:00', '00:00', 900.00, 'ON_SPOT_PAYMENT', 1, 3, 0, 'Physical Fitness, Light Lifting', '+919845012345', 'owner.srilakshmi@catering.com', 'OPEN');

INSERT IGNORE INTO catering_jobs (id, owner_id, title, description, work_type, work_area, detailed_location, job_date, start_time, end_time, payment_amount, payment_type, is_on_spot_payment, workers_required, workers_selected, required_skills, contact_phone, contact_email, status)
VALUES (4, 2, 'Sunday Brunch - Waiters Needed', 'Serving breakfast and lunch at a brunch event. Smart casual dress code. Tips included.', 'WAITER', 'HSR Layout', 'Cafe Terrace, 78, 27th Main, HSR Layout Sector 2, Bangalore', date('now', '+4 days'), '08:00', '14:00', 800.00, 'ON_SPOT_PAYMENT', 1, 5, 0, 'Wait Service, Polite Communication', '+919845098765', 'owner.royal@catering.com', 'OPEN');

INSERT IGNORE INTO catering_jobs (id, owner_id, title, description, work_type, work_area, detailed_location, job_date, start_time, end_time, payment_amount, payment_type, is_on_spot_payment, workers_required, workers_selected, required_skills, contact_phone, contact_email, status)
VALUES (5, 1, 'Post-Event Cleaning Crew', 'Post-wedding cleanup including dishwashing, floor mopping, and table clearing. Gloves provided.', 'CLEANER', 'BTM Layout', 'Royal Orchid Banquet, 34, 16th Main, BTM Layout 2nd Stage, Bangalore', date('now', '+5 days'), '22:00', '02:00', 700.00, 'ON_SPOT_PAYMENT', 1, 4, 0, 'Cleaning, Dishwashing', '+919845012345', 'owner.srilakshmi@catering.com', 'OPEN');
