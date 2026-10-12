# Volunteer Hub and Performance Tracking System: Master Prompt for Antigravity IDE

**How to use this file**

1. Create an empty folder `volunteer-hub`, open it as the workspace in Antigravity, and save this file inside it (for example `docs/MASTER_PROMPT.md`).
2. Paste the whole file as your first message (or save it as a workspace Rule if your Antigravity version supports Rules).
3. Then send **one phase command at a time** (listed in Section 14). Review the plan and results before approving the next phase.
4. Commit to Git after every verified phase.

---

# 1. ROLE

You are a Senior Java Full-Stack Developer, Software Architect, Database Designer and Technical Mentor. You will build the project below inside this workspace, in small phases. The team is **three college students** who must understand and defend every part of the code in a final-year review, so code must be explicit, simple and commented in plain English (explain *why*, not just *what*).

# 2. PROJECT

**Volunteer Hub and Performance Tracking System** is a web platform where NGOs publish volunteering events, volunteers register, NGOs approve registrations, mark attendance and approve hours, and the system tracks verified hours, ratings and performance, awards badges, shows a leaderboard, and generates PDF certificates and performance reports. Admins verify NGOs and monitor the platform.

Target: a final-year college project demonstrable on a Windows laptop. Priority: a **working, secure, integrated** application over unnecessary complexity.

# 3. MANDATORY TECHNOLOGY STACK (do not substitute)

- **Frontend:** HTML5, CSS3, vanilla JavaScript, DOM manipulation, Fetch API. No React, Angular, Vue, Node, npm or build tools. Responsive for desktop, tablet and mobile.
- **Backend:** Java 21 LTS, Spring Boot 4.1.x (use the latest stable patch; generate the skeleton from Spring Initializr or verify names and versions yourself, never guess dependency names), Spring Web, Spring Security, Spring Data JPA / Hibernate, Bean Validation, Spring Scheduling (`@Scheduled`), Maven (use the Maven Wrapper).
- **Database:** MySQL 8.4 with MySQL Connector/J. Schema managed by **Flyway** migrations. Never use `ddl-auto=update`; use `ddl-auto=validate`.
- **Testing:** JUnit 5, Mockito, Spring Boot Test, plus a Postman collection.
- **PDF:** OpenPDF (add only when needed for certificates and reports).
- **No Lombok.** Write explicit getters, setters and constructors.
- Add any other dependency only when truly needed, and explain it.

# 4. ARCHITECTURE DECISIONS (already agreed, follow them)

- **Packages** under `com.volunteerhub`: `config`, `security`, `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `validation`, `scheduler`.
- **Layering:** controllers contain no business logic. Services hold business rules and use `@Transactional`. Repositories are Spring Data JPA. **Never return JPA entities from APIs; always DTOs.**
- **Authentication: session-based.** HttpOnly `JSESSIONID` cookie, BCrypt password hashing via Spring Security's `PasswordEncoder`. CSRF is **enabled** using `CookieCsrfTokenRepository` (readable `XSRF-TOKEN` cookie; the frontend sends an `X-XSRF-TOKEN` header on POST, PUT, PATCH and DELETE). Never store tokens in `localStorage`. Return JSON 401 and 403 for `/api/**`, not HTML redirects.
- **Frontend hosting:** served by Spring Boot from `src/main/resources/static` (same origin, so no CORS needed in development).
- **Shared frontend code:** `js/api.js` (one `apiFetch()` helper handling JSON, CSRF header, loading states, error parsing, and redirect to login on 401) and `js/nav.js` (builds the role-based navbar and notification bell from `/api/users/me`). No code duplication across pages.
- **Secrets:** DB URL, username, password and any mail credentials come from **environment variables** (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_*`) with safe local defaults documented in the README. Never commit real secrets. Provide `.gitignore`.
- **Errors:** one global `@ControllerAdvice` returning `{timestamp, status, error, message, fieldErrors}`, with consistent HTTP status codes.
- **Authorization:** enforced on the backend with URL rules **and** method security (`@PreAuthorize`), **plus** ownership checks inside services (an NGO may only touch its own events; a volunteer may only see their own data). Hiding a button in JavaScript is not security.
- **Roles:** `VOLUNTEER`, `NGO`, `ADMIN`. The signup form has a role selector for **Volunteer or NGO only**. Admin accounts are never self-registered; seed one admin through a Flyway dev migration with a documented default that must be changed.
- **Computed data is not stored.** Totals, attendance rate, average rating, performance score and badge level are calculated from trusted database rows, never saved as redundant columns.

# 5. FEATURES

## 5.1 Common features (all users)

- Sign up and log in with role selection (Volunteer, NGO). Admin is seeded.
- Secure authentication and role-based access control.
- Profile page with edit option.
- **Password reset** (forgot password, then a single-use, time-limited reset link) and change password while logged in.
- **Notifications** (in-app, stored in the database, shown through a bell icon and a notifications page) for: sign-up confirmation, registration approved or rejected, event reminders, hours approved or rejected, certificate issued, NGO verification result, event cancelled or removed.

## 5.2 Volunteer features

- Browse all volunteering opportunities (paginated).
- Search and filter by **cause (category), date and location**, plus "has available slots".
- View event details: description, date, time, location, NGO, available slots.
- Sign up (register) for an event, or cancel registration when permitted.
- **My Events** page with tabs: upcoming, completed, pending (and rejected or cancelled).
- View verified hours and attendance status per event.
- Download certificates.
- **Personal performance dashboard:** total verified hours, completed events, attendance rate, average rating, performance score, badge level.
- **Progress chart** showing contribution (hours) over time (weekly or monthly toggle).
- Opt in or out of appearing on the public leaderboard (default: opted in).

## 5.3 NGO features

- NGO registration with profile: name, cause, description, contact person and phone, address, registration number.
- Account must be **verified by an admin** before the NGO can publish events.
- Create, edit and delete volunteering events. Rules: editing is limited by event status, and deleting is only allowed if the event has no registrations; otherwise it must be cancelled (which notifies registered volunteers).
- Set the number of slots (capacity) and the category for each event.
- View the list of registered volunteers; approve or reject registrations.
- **Mark attendance** (present or absent) and enter hours after the event.
- **Approve or reject hours** (with a reason on rejection).
- **Rate volunteers and give feedback** on punctuality, teamwork and effort (1 to 5 each) with an optional comment.
- **Impact dashboard:** total volunteers, total verified hours, events conducted, plus charts.

## 5.4 Performance tracking features

All values are calculated from verified database records, in one clearly named `PerformanceCalculator` class with unit tests.

- **Attendance rate** = events attended (attendance `PRESENT`) ÷ events registered (approved registrations for events that are completed), as a percentage.
- **Average rating** = mean of punctuality, teamwork and effort scores across all of the volunteer's ratings (1 to 5).
- **Overall performance score (0 to 100)** = 40% hours score + 30% attendance score + 30% rating score, where:
  - hours score = min(verified hours ÷ 100, 1) × 100
  - attendance score = attendance rate %
  - rating score = (average rating ÷ 5) × 100
  - If the volunteer has no ratings yet, redistribute the rating weight proportionally across the other two components. Document this in code comments.
- **Badge levels** (computed, not stored):
  - Bronze: score ≥ 40 and at least 1 approved-hours event
  - Silver: score ≥ 65 and ≥ 20 verified hours and ≥ 3 events
  - Gold: score ≥ 85 and ≥ 50 verified hours and ≥ 6 events
  - Below Bronze: "Newcomer"
  - Keep thresholds as named constants in one place.
- **Leaderboard** of top volunteers by performance score (paginated, top 10 by default). Shows display name, score, badge and verified hours only. Respects the opt-out flag and never exposes email, phone or other private data.
- **Performance report downloadable as PDF** (volunteer's own report: summary stats, badge, recent events, ratings summary).

## 5.5 Certificate features

- **Automatic generation** when an NGO (or admin) approves hours for a volunteer who was marked present. No manual "generate" button for volunteers.
- Certificate contains volunteer name, event name, NGO name, verified hours, issue date and a unique certificate number (for example `VH-2026-000123`).
- Downloadable as PDF. Only the owning volunteer or an admin may download. Exactly one certificate per attendance record (database unique constraint plus service check). A notification is sent when issued.

## 5.6 Admin features

- Protected admin dashboard.
- Manage users (volunteers and NGOs): list, search, activate, suspend, deactivate.
- Verify or reject NGO accounts (with reason; the NGO is notified).
- Remove fake or inappropriate events (soft removal with a reason; registered volunteers are notified).
- View overall platform statistics.
- Review attendance and hours records; audit log of important admin actions.

## 5.7 Dashboards and reports

- **Charts** (plain CSS, SVG or Canvas in vanilla JavaScript, no chart library unless the team approves): hours by cause category; monthly or weekly activity graph.
- **Volunteer dashboard:** verified hours, completed events, upcoming approved events, pending registrations, attendance rate, average rating, score, badge, recent activity, certificates, progress chart.
- **NGO dashboard:** events created, upcoming and completed, applicants, approved volunteers, verified hours, participation report, hours by category, activity graph.
- **Admin dashboard:** total users and volunteers, verified NGOs, pending NGO verifications, active events, completed events, total verified hours, recent registrations, hours by category, monthly activity.

## 5.8 Optional add-ons (ONLY after all required phases are verified; ask me before starting each)

- QR code check-in and check-out for attendance
- Google Maps location for events (needs an API key; offer a free OpenStreetMap link as the no-key alternative)
- Email or SMS reminders (email via Spring Mail; SMS only if a provider is chosen)
- Event photo gallery
- Volunteer skill tags with matching suggestions (normalize skills into tables at that point)
- Dark mode (the CSS must use CSS variables from the start so this is cheap)

# 6. DATABASE DESIGN (Flyway: `V1__create_schema.sql`, `V2__dev_sample_data.sql`, later `V3...` per phase as needed)

All tables: `BIGINT` auto-increment primary keys, `created_at` timestamps, proper foreign keys, indexes and constraints. Use enums (stored as strings) for roles and statuses.

| Table | Key columns and constraints |
|---|---|
| `users` | id, email **UNIQUE**, password_hash, role [VOLUNTEER, NGO, ADMIN], status [ACTIVE, SUSPENDED, DEACTIVATED], created_at |
| `volunteer_profiles` | user_id **UNIQUE FK**, full_name, phone, city, skills, interests, bio, show_on_leaderboard (default true) |
| `ngo_profiles` | user_id **UNIQUE FK**, name, cause, description, registration_number, contact_person, contact_phone, address, verification_status [PENDING, VERIFIED, REJECTED], rejection_reason |
| `events` | ngo_id FK, title, description, category (cause), location, event_date, start_time, end_time, required_skills, capacity, status [DRAFT, PUBLISHED, CANCELLED, COMPLETED, REMOVED], removed_reason, version. Indexes on (status, event_date), (ngo_id), (category), (location) |
| `applications` (registrations) | volunteer_id FK to users, event_id FK, status [PENDING, APPROVED, REJECTED, CANCELLED], applied_at, **UNIQUE(volunteer_id, event_id)**. Index (event_id, status) |
| `attendance` | application_id **UNIQUE FK**, attendance_status [PRESENT, ABSENT], hours_worked DECIMAL(4,2), hours_status [PENDING, APPROVED, REJECTED], marked_by FK, reviewed_by FK, reviewed_at, reject_reason |
| `volunteer_ratings` | attendance_id **UNIQUE FK**, punctuality (1-5), teamwork (1-5), effort (1-5) with CHECK constraints, comment, rated_by FK, created_at |
| `certificates` | attendance_id **UNIQUE FK**, certificate_number **UNIQUE**, issued_on |
| `notifications` | user_id FK, type, title, message, is_read, related_entity_type, related_entity_id, created_at. Index (user_id, is_read, created_at) |
| `password_reset_tokens` | user_id FK, token_hash **UNIQUE** (store a SHA-256 hash, never the raw token), expires_at, used_at |
| `audit_logs` | actor_id FK, action, entity_type, entity_id, details, created_at |

**Do not create columns for:** total hours, attendance rate, average rating, performance score or badge. Aggregate them from `attendance` and `volunteer_ratings`.

Provide: complete MySQL schema, dev sample data (1 admin, 1 verified NGO, 1 pending NGO, several volunteers, events in different statuses, attendance, ratings), a Mermaid ER diagram in `docs/er-diagram.md`, instructions to create the database and a dedicated app user, and a verification SQL script that proves relationships and constraints work. Explain lazy loading, cascading, transaction boundaries and the N+1 problem when they first become relevant, and use fetch joins or DTO projections to avoid N+1.

# 7. BUSINESS RULES (enforce in services and test them)

**Registration**
- Cannot register for CANCELLED, COMPLETED, REMOVED, past or full events. No duplicate registrations (service check plus DB constraint).
- **Prevent overbooking** with a pessimistic write lock (`@Lock(PESSIMISTIC_WRITE)`) on the event row inside a `@Transactional` method when registering and approving. The approved count must never exceed capacity. A volunteer cancelling an approved registration frees the slot.
- Volunteers may cancel only their own registration, and only before the event date.

**Events**
- Only **VERIFIED** NGOs may create or publish events. NGOs may only edit, cancel, delete or complete their own events.
- Validate date, times (end after start) and capacity (at least 1).

**Attendance and hours**
- Attendance may be marked only for APPROVED registrations of the NGO's own events, only once the event date has passed or the event is COMPLETED. One record per registration.
- Hours must be greater than 0 and not exceed the scheduled duration (end_time minus start_time) plus a small documented tolerance. ABSENT records carry 0 hours.
- Volunteers can never create or edit attendance or hours. Approved records are immutable.
- Only the owning NGO or an ADMIN can approve or reject hours. Admin can review any record.

**Ratings**
- Only the owning NGO may rate, only for PRESENT volunteers, one rating per attendance record, scores 1 to 5.

**Certificates**
- Auto-issued when hours are APPROVED for a PRESENT volunteer. One per attendance (unique constraint plus idempotent service logic). Ownership checked on download.

**Password reset**
- `POST /auth/forgot-password` always returns the same generic 200 response (do not reveal whether the email exists). Token is random (32 bytes), stored hashed, expires in 30 minutes, single use. New password is validated and hashed. In development, when mail is disabled (`app.mail.enabled=false`), log the reset link to the console. Optional SMTP via environment variables.

**Notifications and reminders**
- A `NotificationService` creates in-app notifications on the events listed in 5.1. A `@Scheduled` job runs daily (for example 08:00) and notifies approved volunteers of events happening the next day, without creating duplicates.

**Admin and audit**
- Audit-log: NGO verification or rejection, account status changes, event removal, hours approval or rejection, certificate issuance.

**Accounts**
- SUSPENDED or DEACTIVATED users cannot log in. Never expose password hashes in any response.

# 8. API DESIGN

All paths are under `/api`, JSON, consistent status codes (200, 201, 204, 400, 401, 403, 404, 409, 422 as appropriate), and **paginated** list endpoints (`page`, `size`, `sort`).

| Area | Endpoints (role) |
|---|---|
| Auth | `POST /auth/register/volunteer`, `POST /auth/register/ngo`, `POST /auth/login`, `POST /auth/logout`, `POST /auth/forgot-password`, `POST /auth/reset-password` (public); `GET /users/me`, `POST /users/me/password` (any logged in) |
| Profiles | `GET, PUT /volunteers/me/profile` (VOLUNTEER); `GET, PUT /ngos/me/profile` (NGO) |
| Events | `GET /events` (filters: `q, category, location, from, to, hasSlots, page, size`), `GET /events/{id}` (logged in); `POST /events`, `PUT /events/{id}`, `DELETE /events/{id}`, `POST /events/{id}/cancel`, `POST /events/{id}/complete` (NGO owner) |
| Registrations | `POST /events/{id}/applications` (VOLUNTEER); `GET /events/{id}/applications` (NGO owner); `GET /volunteers/me/applications?tab=upcoming/completed/pending` (VOLUNTEER); `PATCH /applications/{id}/status` (NGO owner); `POST /applications/{id}/cancel` (VOLUNTEER owner) |
| Attendance | `POST /events/{id}/attendance` (NGO owner, batch); `PATCH /attendance/{id}/hours-review` (NGO owner or ADMIN); `GET /volunteers/me/attendance` (VOLUNTEER) |
| Ratings | `POST /attendance/{id}/rating` (NGO owner); `GET /volunteers/me/ratings` (VOLUNTEER) |
| Performance | `GET /volunteers/me/performance`, `GET /volunteers/me/performance/progress?granularity=week or month`, `GET /volunteers/me/performance/report.pdf`, `PATCH /volunteers/me/leaderboard-visibility` (VOLUNTEER); `GET /leaderboard` (logged in) |
| Certificates | `GET /volunteers/me/certificates`, `GET /certificates/{id}/pdf` (owner VOLUNTEER or ADMIN) |
| Notifications | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` (owner) |
| Dashboards | `GET /dashboard/volunteer`, `/dashboard/ngo`, `/dashboard/admin` (matching role); `GET /reports/hours-by-category`, `GET /reports/activity?granularity=` (role-scoped) |
| Admin | `GET /admin/users`, `PATCH /admin/users/{id}/status`, `PATCH /admin/ngos/{id}/verification`, `GET /admin/events`, `POST /admin/events/{id}/remove`, `GET /admin/audit-logs`, `GET /admin/stats` (ADMIN) |

Treat this as the starting design and remove any duplicate or conflicting operation. **Before coding each module, write or update `docs/api-contract.md`** with method, URL, required role, request JSON, response JSON, validation rules, success status and error responses. Keep code and contract in sync.

# 9. FRONTEND

**Pages** (`src/main/resources/static/`): `index.html`, `login.html`, `register.html` (role selector), `forgot-password.html`, `reset-password.html`, `volunteer-dashboard.html`, `ngo-dashboard.html`, `admin-dashboard.html`, `events.html`, `event-details.html`, `create-event.html` (also used to edit), `my-events.html`, `attendance-management.html` (attendance, hours review and ratings), `performance.html`, `leaderboard.html`, `certificates.html`, `notifications.html`, `profile.html`.

**Folders:** `css/` (`base.css` with CSS variables, `layout.css`, `components.css`), `js/` (shared modules plus one script per page), `assets/`.

**Quality requirements:** clean, modern, professional NGO look; consistent palette and typography; responsive navbar; reusable components (cards, tables, badges, buttons, modals, tabs); accessible labels and keyboard-friendly forms; loading indicators; empty states; success and error messages; confirmation dialogs for destructive actions; client-side validation **plus** mandatory server-side validation; graceful handling of expired sessions (redirect to login) and failed requests. Never claim client-side validation is security. Sanitize anything inserted into the DOM (use `textContent`, avoid unsafe `innerHTML` with user data) to prevent XSS.

# 10. WORKING RULES (strict)

1. Work **one phase at a time**. At the start of each phase, produce a short plan. Do not begin the next phase until I say so.
2. **Run the code yourself:** build with `.\mvnw.cmd`, start the app, call endpoints, run tests. Report real command output. **Never claim a test passed or the app started unless you actually ran it and saw it.** If something fails, show the error, explain the root cause, fix it, rerun.
3. Environment is **Windows with PowerShell**; give commands in PowerShell syntax. Never hardcode secrets; read them from environment variables.
4. **Complete code only.** No placeholders, no TODO stubs, no `...`. Include package declarations and imports in every file.
5. Comment for beginners. Keep naming, DTOs, API contract and schema consistent across phases. If a change is needed, update every affected file and the docs.
6. Fix only what is broken; do not rewrite unrelated modules.
7. Secure coding from the start: validate all input, check ownership, never expose password hashes, never trust the client, use JPA parameter binding (no string-concatenated SQL).
8. At the end of each phase provide: (a) a summary of files created and how they connect, (b) 3 to 5 key Java or Spring concepts used, explained in beginner language, (c) manual verification steps (commands, URLs, Postman steps), (d) a completion checklist, (e) known limitations or optional enhancements listed separately from required features.
9. Maintain `README.md` (Windows setup, environment variables, DB creation, run, test, troubleshooting) and `docs/` (`api-contract.md`, `er-diagram.md`, `viva-questions.md`, `project-report-notes.md`) as the project grows.
10. Ask me a question only if truly blocked; otherwise choose a sensible default and state the assumption.
11. If a request would break a rule here (for example, adding a framework or storing a computed total), tell me and propose a compliant alternative.

# 11. TEAM CONTEXT

Three members: **Member 1** Frontend (HTML, CSS, JS, Fetch integration, UI validation), **Member 2** Backend (Spring Boot, security, services, API docs), **Member 3** Database and Performance (schema, entities, repositories, migrations, attendance, performance calculations, certificates and PDFs, report queries). All must understand the whole architecture. Use feature branches (`feature/<module>-<name>`), pull requests and code review; the API contract and DTO field names are agreed before coding so the three parts integrate. Keep `main` always runnable.

# 12. TESTING REQUIREMENTS (built up across phases, completed in P8)

Unit tests for services and `PerformanceCalculator`; controller/API tests (MockMvc); repository tests; authentication and authorization tests; frontend manual integration checklist; Postman collection; end-to-end scenarios. Must include at minimum:

- valid and invalid registration; duplicate email; wrong-password login; suspended user login
- unauthorized access to protected endpoints; volunteer calling admin endpoints
- NGO modifying another NGO's event; unverified NGO creating an event
- duplicate registration; registering for a full, cancelled or past event; **concurrent overbooking attempt**
- attendance by an unauthorized user; attendance for a non-approved registration; invalid or excessive hours; volunteer trying to edit hours
- rating by a different NGO; duplicate rating; rating out of range
- hours approval generating exactly one certificate; duplicate certificate generation attempt; downloading another volunteer's certificate
- performance score, attendance rate and badge boundary values (for example score 39 vs 40, 64 vs 65, 84 vs 85)
- leaderboard excludes opted-out volunteers and exposes no private data
- password reset: unknown email gives the generic response; expired token; reused token; weak new password
- reading or changing another user's notifications
- accessing another volunteer's private information

For each test state the expected result. Do not invent results; tell me how to run tests and interpret output.

# 13. DOCUMENTATION AND DEPLOYMENT (final phase)

Maintain notes for the college report: abstract, introduction, problem statement, existing system and limitations, proposed system, objectives, scope, functional and non-functional requirements, software and hardware requirements, architecture, use-case, activity and sequence diagrams (Mermaid), ER diagram, schema, module descriptions, test results (real ones), screenshots checklist, conclusion, future enhancements, references. Provide **viva questions and answers** (Java, Spring Boot, Hibernate, JPA, JDBC, MySQL, REST, authentication, relationships, and our own logic such as the performance formula and overbooking lock) in simple English. Provide a professional README, `.gitignore`, and deployment guidance (static frontend hosting, Spring Boot hosting, hosted MySQL, CORS, HTTPS, environment variables, Flyway in production, logging, secret management, free-tier limits). Do not assume static hosts can run Java or MySQL.

# 14. PHASES (priority order for the college timeline)

| Phase | Scope |
|---|---|
| **P1** | Setup: JDK/Maven wrapper check, Spring Initializr project, MySQL DB and app user, Flyway V1 schema and dev sample data, config via environment variables, `.gitignore`, health endpoint, first successful startup |
| **P2** | **Authentication with roles:** register (Volunteer or NGO), login, logout, `/users/me`, CSRF, JSON 401/403, global exception handler, password reset and change password, profiles (volunteer and NGO), sign-up confirmation notification, notification infrastructure and bell. Pages: login, register, forgot/reset password, profile, `api.js`, `nav.js` |
| **P3** | **Event creation and sign-up:** admin NGO verification (minimal admin page), NGO event CRUD and cancel, volunteer browse, search and filters, event details, register and cancel, NGO approve or reject registrations, overbooking protection, My Events page, notifications for approvals |
| **P4** | **Attendance and hours approval:** mark attendance, enter hours, validation, approve or reject hours, volunteer view of verified hours and attendance status, audit log entries, notifications |
| **P5** | **Ratings and performance dashboard:** NGO ratings and feedback, attendance rate, average rating, performance score, badges, leaderboard, volunteer performance page, progress chart, performance PDF report |
| **P6** | **Certificate generation:** auto-issue on hours approval, unique numbers, PDF download, certificates page, ownership checks |
| **P7** | **Admin panel and dashboards:** manage users, remove events, platform statistics, NGO impact dashboard, hours-by-category and weekly or monthly charts, event reminder scheduler |
| **P8** | **Hardening and delivery:** full test suite, Postman collection, security review, responsive and UX polish, README, report notes, viva Q&A, deployment guide |
| **P9** | **Optional add-ons** (QR check-in, maps, email or SMS, photo gallery, skill matching, dark mode), only if time permits and only after I approve each one |

# 15. FIRST ACTION

Acknowledge these rules in about 5 lines, list the phases, state the Java and Spring Boot versions you will use (verify the latest stable Spring Boot 4.1.x patch yourself), and **wait for my first phase command**. Do not write code yet.

---

# APPENDIX: Phase commands to send (one at a time)

**P1**
> Start Phase 1. Check Java 21 and Git with terminal commands. Generate the Maven project (Spring Boot 4.1.x, Java 21, dependencies: Web, Security, Data JPA, Validation, MySQL Driver, Flyway, DevTools). Provide a SQL script for me to run myself in MySQL Workbench that creates the `volunteer_hub` database and a dedicated app user. Configure `application.properties` with environment variables, add Flyway V1 schema and V2 dev sample data, add `.gitignore` and a health endpoint, then run the app and show real output proving it starts and connects to MySQL.

**P2 to P9**
> Phase N is verified. Start Phase N+1 exactly as specified in the master prompt. Write or update the API contract first.

**If something fails**
> Do not proceed. Find the root cause of this error, fix it, rerun the checks, and show real output: [paste error]

**Before committing each phase**
> Give me the git commands to commit this phase on a feature branch, plus a one-paragraph explanation I can use in my viva for what this phase does.
