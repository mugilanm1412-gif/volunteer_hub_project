-- ============================================================================
-- Volunteer Hub and Performance Tracking System
-- Flyway Migration V2: Development Seed & Sample Data
-- ============================================================================
-- Password for all seeded demo accounts is: password123
-- BCrypt Hash: $2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG
-- ============================================================================

-- 1. Insert Seed Users
INSERT INTO users (id, email, password_hash, role, status, created_at) VALUES
(1, 'admin@volunteerhub.org', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'ADMIN', 'ACTIVE', NOW()),
(2, 'contact@greenearth.org', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'NGO', 'ACTIVE', NOW()),
(3, 'help@oceanrescue.org', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'NGO', 'ACTIVE', NOW()),
(4, 'alice@example.com', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'VOLUNTEER', 'ACTIVE', NOW()),
(5, 'bob@example.com', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'VOLUNTEER', 'ACTIVE', NOW()),
(6, 'charlie@example.com', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'VOLUNTEER', 'ACTIVE', NOW()),
(7, 'diana@example.com', '$2a$10$4Go0H9rOb053IA1Eip5A6uYluQ.748wwRRCbOO5S0o5FMRRgLHZxG', 'VOLUNTEER', 'ACTIVE', NOW());

-- 2. Insert NGO Profiles (1 Verified, 1 Pending Verification)
INSERT INTO ngo_profiles (id, user_id, name, cause, description, registration_number, contact_person, contact_phone, address, verification_status, rejection_reason, created_at) VALUES
(1, 2, 'Green Earth Foundation', 'Environment', 'Dedicated to urban reforestation, clean oceans, and environmental stewardship across local communities.', 'NGO-REG-2024-001', 'Elena Vance', '+1-555-0101', '124 Green Boulevard, Eco City', 'VERIFIED', NULL, NOW()),
(2, 3, 'Ocean Rescue Initiative', 'Marine Conservation', 'Focuses on coastal conservation, marine debris cleanup, and marine habitat preservation.', 'NGO-REG-2025-089', 'Marcus Brody', '+1-555-0102', '45 Marina Pier, Coast City', 'PENDING', NULL, NOW());

-- 3. Insert Volunteer Profiles (Including public opt-in and opt-out cases)
INSERT INTO volunteer_profiles (id, user_id, full_name, phone, city, skills, interests, bio, show_on_leaderboard, created_at) VALUES
(1, 4, 'Alice Smith', '+1-555-0201', 'Eco City', 'Tree planting, Team leadership, First aid', 'Environment, Animal Welfare', 'Passionate environmentalist and weekend tree planter.', TRUE, NOW()),
(2, 5, 'Bob Johnson', '+1-555-0202', 'Eco City', 'Logistics, Public speaking, Event setup', 'Community Aid, Education', 'Avid volunteer eager to support local communities.', TRUE, NOW()),
(3, 6, 'Charlie Davis', '+1-555-0203', 'Coast City', 'Web development, Photography', 'Education, Technology', 'Prefers quiet contributions; opted out of public rankings.', FALSE, NOW()),
(4, 7, 'Diana Prince', '+1-555-0204', 'Eco City', 'Teaching, Coordination, Art therapy', 'Youth Empowerment, Art', 'Committed to empowering youth through creative workshops.', TRUE, NOW());

-- 4. Insert Events (COMPLETED, PUBLISHED, CANCELLED, DRAFT)
INSERT INTO events (id, ngo_id, title, description, category, location, event_date, start_time, end_time, required_skills, capacity, status, removed_reason, version, created_at) VALUES
(1, 2, 'Coastal Beach Cleanup Drive', 'Join us to remove plastic waste and debris from the public shoreline to protect marine life.', 'Environment', 'Sunshine Beach, Eco City', DATE_SUB(CURDATE(), INTERVAL 14 DAY), '08:30:00', '12:30:00', 'Physical stamina, Teamwork', 20, 'COMPLETED', NULL, 1, NOW()),
(2, 2, 'Urban Reforestation & Sapling Care', 'Planting 300 native tree saplings in the urban green belt. Gloves and tools provided.', 'Environment', 'Metro Green Belt Park', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '09:00:00', '13:00:00', 'Gardening, Enthusiasm', 15, 'PUBLISHED', NULL, 0, NOW()),
(3, 2, 'Community Food Relief Packing', 'Sorting, inspecting, and packing nutritional food hampers for vulnerable families.', 'Community Aid', 'Central Food Depot, Hall 2', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00', '14:00:00', 'Attention to detail, Sorting', 10, 'PUBLISHED', NULL, 0, NOW()),
(4, 2, 'Community Center Wall Painting', 'Revitalizing community hall walls with child-friendly educational murals.', 'Arts & Culture', 'North District Center', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00:00', '15:00:00', 'Painting', 8, 'CANCELLED', 'Heavy rainfall caused site flooding.', 1, NOW()),
(5, 2, 'Renewable Energy Youth Seminar', 'Interactive workshop introducing teenagers to clean solar energy and sustainability principles.', 'Education', 'Public Library Auditorium', DATE_ADD(CURDATE(), INTERVAL 20 DAY), '14:00:00', '17:00:00', 'Public speaking, Science tutoring', 25, 'DRAFT', NULL, 0, NOW());

-- 5. Insert Applications (Registrations)
INSERT INTO applications (id, volunteer_id, event_id, status, applied_at) VALUES
(1, 4, 1, 'APPROVED', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(2, 5, 1, 'APPROVED', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(3, 6, 1, 'APPROVED', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(4, 4, 2, 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 7, 2, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 5, 3, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 6. Insert Attendance & Hours Records for Completed Event 1
INSERT INTO attendance (id, application_id, attendance_status, hours_worked, hours_status, marked_by, reviewed_by, reviewed_at, reject_reason, created_at) VALUES
(1, 1, 'PRESENT', 4.00, 'APPROVED', 2, 2, DATE_SUB(NOW(), INTERVAL 13 DAY), NULL, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(2, 2, 'PRESENT', 4.00, 'APPROVED', 2, 2, DATE_SUB(NOW(), INTERVAL 13 DAY), NULL, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(3, 3, 'ABSENT', 0.00, 'REJECTED', 2, 2, DATE_SUB(NOW(), INTERVAL 13 DAY), 'Volunteer did not arrive on site.', DATE_SUB(NOW(), INTERVAL 14 DAY));

-- 7. Insert Volunteer Ratings (Punctuality, Teamwork, Effort rated by NGO)
INSERT INTO volunteer_ratings (id, attendance_id, punctuality, teamwork, effort, comment, rated_by, created_at) VALUES
(1, 1, 5, 5, 5, 'Alice was punctual, exceptionally proactive, and motivated the whole team!', 2, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(2, 2, 4, 5, 4, 'Bob demonstrated wonderful teamwork and maintained great spirits throughout the drive.', 2, DATE_SUB(NOW(), INTERVAL 13 DAY));

-- 8. Insert Certificates (Auto-issued on approved hours)
INSERT INTO certificates (id, attendance_id, certificate_number, issued_on) VALUES
(1, 1, 'VH-2026-000001', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(2, 2, 'VH-2026-000002', DATE_SUB(NOW(), INTERVAL 13 DAY));

-- 9. Insert Sample In-App Notifications
INSERT INTO notifications (id, user_id, type, title, message, is_read, related_entity_type, related_entity_id, created_at) VALUES
(1, 4, 'APPLICATION_APPROVED', 'Application Approved', 'Your registration for Coastal Beach Cleanup Drive was approved!', TRUE, 'EVENT', 1, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(2, 4, 'HOURS_APPROVED', 'Volunteer Hours Approved', 'Green Earth Foundation approved 4.00 hours for Coastal Beach Cleanup Drive.', TRUE, 'ATTENDANCE', 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3, 4, 'CERTIFICATE_ISSUED', 'Certificate Issued', 'Your certificate VH-2026-000001 is ready for download.', FALSE, 'CERTIFICATE', 1, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(4, 2, 'NGO_VERIFIED', 'Account Verified', 'Your NGO Green Earth Foundation has been successfully verified by Administrator.', TRUE, 'NGO_PROFILE', 1, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- 10. Insert Audit Log
INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, details, created_at) VALUES
(1, 1, 'VERIFY_NGO', 'NGO_PROFILE', 1, 'Administrator verified Green Earth Foundation (Registration: NGO-REG-2024-001).', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(2, 2, 'APPROVE_HOURS', 'ATTENDANCE', 1, 'Approved 4.00 volunteer hours for Alice Smith.', DATE_SUB(NOW(), INTERVAL 13 DAY));
