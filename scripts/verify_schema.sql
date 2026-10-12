-- ============================================================================
-- Volunteer Hub and Performance Tracking System
-- Schema & Integrity Verification Script
-- ============================================================================
-- Purpose:
--   Runs diagnostic assertions against the 'volunteer_hub' database to confirm
--   that all tables, indexes, foreign keys, and sample data exist and function.
-- ============================================================================

USE volunteer_hub;

-- 1. Count records across all 11 core tables
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'volunteer_profiles', COUNT(*) FROM volunteer_profiles
UNION ALL
SELECT 'ngo_profiles', COUNT(*) FROM ngo_profiles
UNION ALL
SELECT 'events', COUNT(*) FROM events
UNION ALL
SELECT 'applications', COUNT(*) FROM applications
UNION ALL
SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL
SELECT 'volunteer_ratings', COUNT(*) FROM volunteer_ratings
UNION ALL
SELECT 'certificates', COUNT(*) FROM certificates
UNION ALL
SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL
SELECT 'password_reset_tokens', COUNT(*) FROM password_reset_tokens
UNION ALL
SELECT 'audit_logs', COUNT(*) FROM audit_logs;

-- 2. Verify foreign key constraints and joins (User -> Profile -> Event -> Attendance -> Rating -> Certificate)
SELECT 
    u.email AS volunteer_email,
    vp.full_name AS volunteer_name,
    e.title AS event_title,
    np.name AS ngo_name,
    att.attendance_status,
    att.hours_worked,
    att.hours_status,
    vr.punctuality,
    vr.teamwork,
    vr.effort,
    ROUND((vr.punctuality + vr.teamwork + vr.effort) / 3.0, 2) AS average_rating,
    c.certificate_number
FROM attendance att
JOIN applications app ON att.application_id = app.id
JOIN users u ON app.volunteer_id = u.id
JOIN volunteer_profiles vp ON u.id = vp.user_id
JOIN events e ON app.event_id = e.id
JOIN ngo_profiles np ON e.ngo_id = np.user_id
LEFT JOIN volunteer_ratings vr ON att.id = vr.attendance_id
LEFT JOIN certificates c ON att.id = c.attendance_id;

-- 3. Verify Unique Constraint on (volunteer_id, event_id)
-- Duplicate insert attempt should trigger error code 1062: Duplicate entry
-- INSERT INTO applications (volunteer_id, event_id, status) VALUES (4, 1, 'PENDING');
