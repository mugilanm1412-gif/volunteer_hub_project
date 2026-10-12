# Volunteer Hub - Postman API Collection & Testing Guide

This directory contains the official Postman collection for the **Volunteer Hub and Performance Tracking System**. It covers all REST endpoints across all application modules, with built-in session cookie handling and automated Cross-Site Request Forgery (CSRF) token extraction.

---

## 1. Quick Setup & Import

1. **Open Postman** (Desktop application or web agent).
2. Click **Import** (top-left button in Postman).
3. Select `volunteer-hub.postman_collection.json` from this folder (`docs/postman/`).
4. Once imported, the collection **"Volunteer Hub API"** will appear in your left sidebar.

---

## 2. Environment & Variables

The collection includes built-in collection variables configured for immediate local testing:

| Variable | Default Value | Description |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Root URL of the running Spring Boot server |
| `xsrfToken` | *(auto-populated)* | CSRF token extracted from the `XSRF-TOKEN` cookie |
| `volunteerEmail` | `alice@example.com` | Seeded active volunteer account |
| `volunteerPassword` | `password123` | Password for test volunteer |
| `ngoEmail` | `contact@greenearth.org` | Seeded verified NGO account |
| `ngoPassword` | `password123` | Password for test NGO |
| `adminEmail` | `admin@volunteerhub.org` | Seeded platform administrator account |
| `adminPassword` | `admin123` | Password for test administrator |
| `activeEventId` | `1` | Default event ID for registration & attendance flows |

---

## 3. How CSRF & Session Authentication Works

Spring Security enforces session-based authentication via HTTP-only `JSESSIONID` cookies, and protects all state-mutating requests (`POST`, `PUT`, `PATCH`, `DELETE`) with CSRF tokens.

### Automated CSRF Extraction
The collection defines a global **Collection Pre-request Script** and **Test Script**:
1. When you execute **Auth -> Login** (or any endpoint), the server sets an `XSRF-TOKEN` cookie in the response.
2. The Postman Test Script automatically extracts the cookie value:
   ```javascript
   const xsrfCookie = pm.cookies.get('XSRF-TOKEN');
   if (xsrfCookie) {
       pm.collectionVariables.set('xsrfToken', decodeURIComponent(xsrfCookie));
   }
   ```
3. Every mutating request is pre-configured with the header:
   ```http
   X-XSRF-TOKEN: {{xsrfToken}}
   Content-Type: application/json
   ```
4. Postman's built-in cookie jar automatically stores and forwards the `JSESSIONID` cookie across subsequent requests.

---

## 4. Recommended Execution Workflow

To walk through the full end-to-end lifecycle, execute the folders in the following order:

### Step 1: Health & Discovery
- `GET {{baseUrl}}/api/health` -> Confirms database connectivity, Flyway status, and server uptime.

### Step 2: Authentication (Switching Roles)
- **Login as Volunteer**: Execute `Auth -> Login as Volunteer (Alice)`
  - Verifies credentials and captures Alice's session + CSRF token.
- **Inspect Current User**: Execute `Auth -> Current User (/api/users/me)` -> Returns role `ROLE_VOLUNTEER`.

### Step 3: Volunteer Actions
- **Browse Events**: `Events -> Search & Browse Events (Public)`
- **Apply for Event**: `Applications -> Apply for Event` (Registers Alice for Event #1).
- **View My Events**: `Applications -> My Events (Upcoming / Completed)`.
- **View Performance**: `Performance -> My Performance Summary` (Displays badges, score, and verified hours).
- **Download Performance PDF**: `Performance -> Download Performance Report (PDF)`.
- **View Leaderboard**: `Performance -> Public Leaderboard`.
- **View Certificates**: `Certificates -> My Certificates`.

### Step 4: Login as NGO
- Execute `Auth -> Login as NGO (Green Earth)`
- **View NGO Dashboard**: `Dashboards -> NGO Impact Dashboard`.
- **List Applicants**: `Applications -> List Applications for Event #1`.
- **Approve Registration**: `Applications -> Approve Application (Pessimistic Lock Protected)`.
- **Record Attendance**: `Attendance -> Record Batch Attendance`.
- **Approve Hours**: `Attendance -> Approve Hours (Auto-issues Certificate)`.
- **Submit Volunteer Rating**: `Ratings -> Rate Volunteer (Punctuality, Teamwork, Effort)`.

### Step 5: Login as Admin
- Execute `Auth -> Login as Admin`
- **View Admin Dashboard**: `Dashboards -> Platform Admin Dashboard`.
- **List NGOs & Verification**: `Admin -> List NGOs` / `Verify NGO`.
- **Search Platform Users**: `Admin -> Search Users`.
- **Inspect Audit Logs**: `Admin -> View Audit Logs`.
- **Trigger Event Reminders**: `Admin -> Trigger Event Reminders Manually`.

---

## 5. Security & Error Testing Scenarios Included

| Scenario | Request | Expected HTTP Status |
|---|---|---|
| Unauthenticated Access | `GET /api/users/me` without cookie | `401 Unauthorized` (JSON) |
| Role Violation | Volunteer calling `/api/admin/stats` | `403 Forbidden` (JSON) |
| Missing CSRF | Mutation request with omitted `X-XSRF-TOKEN` | `403 Forbidden` |
| Overbooking Attempt | Registering for full event | `400 Bad Request` |
| Password Reset with Expired Token | `POST /api/auth/reset-password` | `400 Bad Request` |
| Unauthorized Certificate Download | Accessing another volunteer's PDF | `403 Forbidden` |
