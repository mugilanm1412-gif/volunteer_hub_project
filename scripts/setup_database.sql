-- ============================================================================
-- Volunteer Hub and Performance Tracking System
-- Database & Dedicated User Initialization Script
-- ============================================================================
-- Purpose:
--   1. Creates the 'volunteer_hub' database with full UTF-8 (utf8mb4) support.
--   2. Creates a dedicated application user 'vh_app' restricted to localhost.
--   3. Grants the application user privileges only on the 'volunteer_hub' database.
--
-- Instructions:
--   1. Open MySQL Workbench.
--   2. Connect as 'root' (or another administrative user).
--   3. Open and execute this script (Ctrl + Shift + Enter).
-- ============================================================================

-- Step 1: Create the database
CREATE DATABASE IF NOT EXISTS volunteer_hub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Step 2: Create the dedicated application user
-- The default password configured in application.properties is 'vh_secret_2026'.
-- You can change this password, but remember to update your DB_PASSWORD env var or application.properties accordingly.
CREATE USER IF NOT EXISTS 'vh_app'@'localhost' IDENTIFIED BY 'vh_secret_2026';

-- Step 3: Grant all privileges on the volunteer_hub database to vh_app
GRANT ALL PRIVILEGES ON volunteer_hub.* TO 'vh_app'@'localhost';

-- Step 4: Flush privileges to ensure changes take effect immediately
FLUSH PRIVILEGES;

-- Step 5: Verification queries
SHOW DATABASES LIKE 'volunteer_hub';
SELECT user, host FROM mysql.user WHERE user = 'vh_app';
