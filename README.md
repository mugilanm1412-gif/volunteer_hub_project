# Volunteer Hub and Performance Tracking System

A full-stack web platform built for NGOs, volunteers, and platform administrators. Built with **Spring Boot 4.1.x**, **Java 21 LTS**, **MySQL 8.4 / 8.0**, and clean vanilla web technologies.

---

## 1. Prerequisites (Windows)
- **Java 21 LTS** (Eclipse Temurin or OpenJDK 21)
- **Git**
- **MySQL Server 8.0+** running locally (e.g., MySQL Windows Service `MySQL80`)
- **MySQL Workbench** or MySQL CLI

---

## 2. Environment Variables & Defaults
The application uses externalized configuration with safe local developer fallbacks:

| Variable | Description | Local Developer Default |
|---|---|---|
| `SERVER_PORT` | HTTP port for Spring Boot | `8080` |
| `DB_HOST` | MySQL database host | `localhost` |
| `DB_PORT` | MySQL database port | `3306` |
| `DB_NAME` | Database schema name | `volunteer_hub` |
| `DB_USERNAME` | Application DB user | `vh_app` |
| `DB_PASSWORD` | Application DB password | `vh_secret_2026` |
| `MAIL_ENABLED` | Email notifications toggle | `false` |

To override in PowerShell:
```powershell
$env:DB_USERNAME = "vh_app"
$env:DB_PASSWORD = "vh_secret_2026"
```

---

## 3. Database Setup (MySQL Workbench)
1. Open **MySQL Workbench** and connect as `root`.
2. Open and run the initialization script:
   [`scripts/setup_database.sql`](file:///c:/Users/mugil/OneDrive/Desktop/volenteer_hub/scripts/setup_database.sql)
3. This creates:
   - Database: `volunteer_hub` (Character set: `utf8mb4`)
   - User: `vh_app` with password `vh_secret_2026`
   - Grants: Full permissions on `volunteer_hub.*`

---

## 4. Running the Application
From PowerShell in the project root:
```powershell
# Compile and build
.\mvnw.cmd clean package -DskipTests

# Run the Spring Boot application
.\mvnw.cmd spring-boot:run
```

---

## 5. Verification
Once started, test the health check endpoint:
```powershell
curl.exe -s http://localhost:8080/api/health
```
Expected response:
```json
{
  "service": "Volunteer Hub and Performance Tracking System",
  "version": "1.0.0",
  "status": "UP",
  "database": "CONNECTED",
  "databaseCatalog": "volunteer_hub"
}
```

---

## 6. Seed Accounts (Dev Sample Data)
All demo accounts have password: `password123`

| Role | Email | Name / Org | Status |
|---|---|---|---|
| `ADMIN` | `admin@volunteerhub.org` | System Administrator | Active |
| `NGO` | `contact@greenearth.org` | Green Earth Foundation | Verified |
| `NGO` | `help@oceanrescue.org` | Ocean Rescue Initiative | Pending |
| `VOLUNTEER` | `alice@example.com` | Alice Smith | Active (Leaderboard: Yes) |
| `VOLUNTEER` | `bob@example.com` | Bob Johnson | Active (Leaderboard: Yes) |
| `VOLUNTEER` | `charlie@example.com` | Charlie Davis | Active (Leaderboard: No) |
| `VOLUNTEER` | `diana@example.com` | Diana Prince | Active (Leaderboard: Yes) |
