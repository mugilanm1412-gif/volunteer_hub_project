# Volunteer Hub and Performance Tracking System

A full-stack enterprise web application built for NGOs, volunteers, and platform administrators. Built with **Spring Boot 4.1.x**, **Java 21 LTS**, **MySQL 8.4 / 8.0**, **Flyway**, **OpenPDF**, and pure vanilla web technologies (HTML5, modern CSS3, ES6+ JavaScript, SVG charts).

---

## 1. Project Overview & Key Features

Volunteer Hub solves the coordination, verification, and transparency challenges facing modern community engagement platforms:
- **Role-Based Access Control (RBAC):** Distinct interfaces, permissions, and security contexts for **Volunteers**, **NGO Organizers**, and **Platform Administrators**.
- **Pessimistic Concurrency Locking (`@Lock(PESSIMISTIC_WRITE)`):** Eliminates overbooking race conditions when multiple volunteers concurrently register for high-demand events.
- **Attendance & Verified Service Hours:** Enforces strict event conclusion rules, scheduled duration tolerances, and administrative verification.
- **Dynamic Performance & Badge Calculator:** Computes fair, multi-factor scores ($0–100$) balancing verified hours, attendance rates, and 3-axis NGO ratings (Punctuality, Teamwork, Effort) with mathematical boundary tiers (*Newcomer*, *Bronze*, *Silver*, *Gold*).
- **Automated Digital Certificates:** Auto-issues unique serial-numbered certificates (`CERT-YYYY-XXXXXX`) with cryptographic verification codes and downloadable PDF certificates.
- **Interactive Pure SVG Analytics:** Real-time donut charts and bar graphs rendered purely via SVG without external heavy charting libraries.
- **Enterprise Security Hardening:** Session authentication with HTTP-only cookies, automated CSRF validation, zero database string concatenation, and DOM XSS sanitization.

---

## 2. Technology Stack

- **Backend Runtime:** Java 21 LTS (OpenJDK / Eclipse Temurin)
- **Framework:** Spring Boot 4.1.x (`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`)
- **Database & Migration:** MySQL 8.0 / 8.4 Server, Flyway Database Migrations (`V1__create_schema.sql`, `V2__dev_sample_data.sql`)
- **Connection Pool:** HikariCP
- **PDF Generation:** OpenPDF 2.0.3 (iText fork)
- **Frontend:** Vanilla HTML5, Modern CSS Design System (Custom properties, CSS Grid, Flexbox), ES6+ JavaScript (`Fetch API`, SVG Rendering)
- **Testing:** JUnit 5, Spring Boot Test, MockMvc, Mockito (104 automated tests)

---

## 3. Prerequisites (Windows Setup)

Ensure the following tools are installed on your Windows machine:
1. **Java 21 LTS**: Verify with:
   ```powershell
   java -version
   ```
   *(Should output Java 21.x.x)*
2. **Git**: Verify with:
   ```powershell
   git --version
   ```
3. **MySQL Server 8.0 or 8.4**: Ensure the MySQL Windows service (e.g., `MySQL80`) is running. Verify connectivity via MySQL Workbench or command line:
   ```powershell
   mysql -u root -p -e "SELECT VERSION();"
   ```

---

## 4. Database Setup & Initialization

### Step 1: Execute Initialization Script in MySQL Workbench
1. Open **MySQL Workbench** and connect as the MySQL `root` user.
2. Open the setup script located in [`scripts/setup_database.sql`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/scripts/setup_database.sql).
3. Execute the script to create the database schema and application database user:
   ```sql
   CREATE DATABASE IF NOT EXISTS volunteer_hub
       CHARACTER SET utf8mb4
       COLLATE utf8mb4_unicode_ci;

   CREATE USER IF NOT EXISTS 'vh_app'@'localhost' IDENTIFIED BY 'vh_secret_2026';
   CREATE USER IF NOT EXISTS 'vh_app'@'127.0.0.1' IDENTIFIED BY 'vh_secret_2026';

   GRANT ALL PRIVILEGES ON volunteer_hub.* TO 'vh_app'@'localhost';
   GRANT ALL PRIVILEGES ON volunteer_hub.* TO 'vh_app'@'127.0.0.1';
   FLUSH PRIVILEGES;
   ```

### Step 2: Automatic Schema Migration via Flyway
You do not need to manually run DDL tables! When Spring Boot launches, **Flyway** automatically runs:
- `V1__create_schema.sql`: Creates all 11 core relational tables, indexes, and constraints.
- `V2__dev_sample_data.sql`: Seeds default admin, verified NGOs, volunteers, sample events, attendance records, and ratings.

---

## 5. Configuration & Environment Variables

The application is configured via externalized environment variables with safe local defaults in `application.properties`:

| Variable | Description | Local Developer Default |
|---|---|---|
| `SERVER_PORT` | HTTP port for the web server | `8080` |
| `DB_HOST` | MySQL hostname or IP | `localhost` |
| `DB_PORT` | MySQL connection port | `3306` |
| `DB_NAME` | Database schema name | `volunteer_hub` |
| `DB_USERNAME` | Dedicated application username | `vh_app` |
| `DB_PASSWORD` | Application user password | `vh_secret_2026` |
| `MAIL_ENABLED` | Email notifications toggle | `false` (logs links to console) |
| `EVENT_REMINDER_CRON` | Cron schedule for reminders | `0 0 8 * * ?` (Daily at 8:00 AM) |
| `EVENT_REMINDER_ENABLED` | Scheduler active toggle | `true` |

To set custom environment variables in Windows PowerShell:
```powershell
$env:DB_USERNAME = "vh_app"
$env:DB_PASSWORD = "vh_secret_2026"
$env:SERVER_PORT = "8080"
```

---

## 6. How to Build, Test, and Run

### Running Automated Test Suite
To execute all 104 unit, edge-case, concurrency, and security integration tests:
```powershell
.\mvnw.cmd test
```
*Expected Result: `BUILD SUCCESS`, `Tests run: 104, Failures: 0, Errors: 0, Skipped: 0`.*

### Building the Production Executable
```powershell
.\mvnw.cmd clean package -DskipTests
```
*Creates the standalone JAR in `target/volunteer-hub-0.0.1-SNAPSHOT.jar`.*

### Running the Application Locally
Launch the application using the Maven wrapper:
```powershell
.\mvnw.cmd spring-boot:run
```
Once started, the application is accessible in your browser at:
**`http://localhost:8080`**

Verify server health in PowerShell:
```powershell
curl.exe -s http://localhost:8080/api/health
```

---

## 7. Default Credentials & Sample Accounts

All seeded demo accounts are provisioned with default password: `password123` (Admin: `admin123`).

| Role | Email | Password | Name / Entity | Purpose |
|---|---|---|---|---|
| **ADMIN** | `admin@volunteerhub.org` | `admin123` | System Administrator | Platform governance, NGO verification, user status management |
| **NGO** | `contact@greenearth.org` | `password123` | Green Earth Foundation | Verified organizer with active events, attendance, ratings |
| **NGO** | `help@oceanrescue.org` | `password123` | Ocean Rescue Initiative | Newly registered NGO pending verification |
| **VOLUNTEER** | `alice@example.com` | `password123` | Alice Smith | High-performing volunteer (Silver badge, certificates issued) |
| **VOLUNTEER** | `bob@example.com` | `password123` | Bob Johnson | Active volunteer (Bronze badge) |
| **VOLUNTEER** | `charlie@example.com` | `password123` | Charlie Davis | Active volunteer (Leaderboard opted-out) |
| **VOLUNTEER** | `diana@example.com` | `password123` | Diana Prince | Active volunteer |

---

## 8. How to Change Default Admin Credentials

### Method 1: Via the Web Interface (Recommended)
1. Open `http://localhost:8080/login.html` and sign in with `admin@volunteerhub.org` / `admin123`.
2. Navigate to **Profile** (`http://localhost:8080/profile.html`).
3. Scroll to the **Change Password** section.
4. Enter current password `admin123`, enter your new strong password (min 8 characters), and click **Update Password**.

### Method 2: Via SQL / BCrypt Hash Generation
To reset or change the administrator password directly in MySQL:
1. Generate a BCrypt hash for your desired password (e.g., using Spring Security or an online BCrypt tool with 10+ rounds).
2. Execute the update statement in MySQL Workbench:
   ```sql
   UPDATE volunteer_hub.users
   SET password_hash = '$2a$10$YourGeneratedBCryptHashHere'
   WHERE email = 'admin@volunteerhub.org';
   ```

---

## 9. API Testing with Postman

An end-to-end Postman collection covering all 11 modules and automated CSRF handling is located in:
[`docs/postman/volunteer-hub.postman_collection.json`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/postman/volunteer-hub.postman_collection.json)

For detailed import instructions, environment variables, and execution workflows, see:
[`docs/postman/README.md`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/postman/README.md)

---

## 10. Troubleshooting & Common Issues

### Issue 1: Port 8080 Already in Use
**Symptom:** `Web server failed to start. Port 8080 was already in use.`
**Resolution:** Find and terminate the blocking process, or change the port via PowerShell:
```powershell
$env:SERVER_PORT = "8085"
.\mvnw.cmd spring-boot:run
```

### Issue 2: MySQL Access Denied for User 'vh_app'
**Symptom:** `Access denied for user 'vh_app'@'localhost'`
**Resolution:** Re-run the SQL grant statements in MySQL Workbench as `root`:
```sql
GRANT ALL PRIVILEGES ON volunteer_hub.* TO 'vh_app'@'localhost' IDENTIFIED BY 'vh_secret_2026';
FLUSH PRIVILEGES;
```

### Issue 3: Flyway Migration Checksum Mismatch
**Symptom:** `FlywayException: Validate failed: Migrations have failed validation`
**Resolution:** This occurs if migration scripts were modified after being applied. In development, reset the database cleanly:
```sql
DROP DATABASE volunteer_hub;
CREATE DATABASE volunteer_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Restarting the application will cleanly re-execute Flyway migrations from scratch.

### Issue 4: CSRF Token Missing in Direct API Calls (cURL / Postman)
**Symptom:** State-mutating request returns `HTTP 403 Forbidden`
**Resolution:** Spring Security enforces CSRF on all `POST`, `PUT`, `PATCH`, and `DELETE` requests. First make a GET request to `/api/health` or `/api/auth/login`, extract the `XSRF-TOKEN` cookie, and pass it in the `X-XSRF-TOKEN` header.

---

## 11. Documentation Directory (`docs/`)

- [`docs/api-contract.md`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/api-contract.md) - Full REST API specification (13 sections)
- [`docs/er-diagram.md`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/er-diagram.md) - Mermaid entity-relationship diagram and schema notes
- [`docs/viva-questions.md`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/viva-questions.md) - Comprehensive viva voce technical questions and answers
- [`docs/project-report-notes.md`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/project-report-notes.md) - College project report documentation
- [`docs/postman/`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/docs/postman/) - Postman collection and import guide
