# Volunteer Hub: Master Prompt Part 2 (Phases 4 to 9) for Antigravity IDE

**Use this file after Phase 3 (3a, 3b, 3c) is verified and committed.**

## How to use this file

1. Save it in the project as `docs/MASTER_PROMPT_PART2.md`. The original file `docs/MASTER_PROMPT.md` must stay in the project, because it holds the full stack, schema, business rules, API design and working rules.
2. Open a **new Antigravity conversation**. Attach both files. Paste **Section A (Handoff prompt)** below as the first message and wait for the agent's audit.
3. Then send **one phase command at a time** from Section C. Verify, commit, and start a fresh conversation every one or two sub-phases (re-attach both files each time).
4. If the agent stalls, use the recovery prompts in Section D.

---

# SECTION A: Handoff prompt (paste this first)

```text
You are continuing the "Volunteer Hub and Performance Tracking System" project in this workspace. Phases 1, 2 and 3 are complete and verified. The attached docs/MASTER_PROMPT.md is still the authority for the stack, architecture, database design, business rules, API design, frontend rules and working rules. Everything in it still applies, especially:

- Java 21, Spring Boot 4.1.x, Maven wrapper, MySQL 8.4, Flyway (ddl-auto=validate, never update), vanilla HTML/CSS/JS, no Lombok, no React/Node.
- Session-based auth with CSRF, DTOs only (never return entities), controllers without business logic, @Transactional services, @PreAuthorize plus ownership checks.
- Computed data (total hours, attendance rate, average rating, performance score, badge) is NEVER stored; it is calculated from the attendance and volunteer_ratings tables.
- Windows PowerShell commands, secrets from environment variables, complete code only (no placeholders, no TODO, no "..."), beginner-friendly comments.
- Run the code yourself and show real output. Never claim a build, test or endpoint worked unless you ran it and saw it.

FIRST ACTION (do not write any feature code yet):
1. Audit the repo. Run `git status`, `git log --oneline -15`, `.\mvnw.cmd clean test`, and list the files under src/main/java, src/main/resources/db/migration and src/main/resources/static.
2. Read docs/api-contract.md, docs/er-diagram.md and the existing Flyway migrations.
3. Report in under 25 lines: (a) what exists from Phases 1 to 3, (b) the current Flyway version, (c) the existing entities, services, controllers and pages, (d) anything broken or missing from Phases 1 to 3, (e) the exact names of the existing shared helpers (apiFetch, nav.js, NotificationService, global exception handler, security config) that later phases must reuse.
4. Do NOT rewrite or restructure anything that already works. Reuse existing classes, DTO naming style, package layout and CSS components.
5. Then list the remaining phases (P4 to P9) and WAIT for my phase command.
```

---

# SECTION B: Rules that apply to every phase command below

These are already in the master prompt, repeated here because agents forget them in long sessions.

1. Work on **one sub-phase only**, then stop and wait for me.
2. Start each sub-phase with a short plan (maximum 10 lines).
3. **Update `docs/api-contract.md` first**, then code.
4. Add a new Flyway migration (`V4__...`, `V5__...`) only if the schema needs a change. Never edit an already-applied migration.
5. Run `.\mvnw.cmd clean test`, start the app, and call the new endpoints. Show real output.
6. End every sub-phase with: (a) files created or changed and how they connect, (b) 3 to 5 Java/Spring concepts in beginner language, (c) manual verification steps, (d) a completion checklist, (e) known limitations kept separate from required features.
7. Fix only what is broken. Do not rewrite unrelated modules.
8. Add the relevant tests from Section 12 of the master prompt as you go; do not leave all testing for Phase 8.

---

# SECTION C: Phase commands (send one at a time)

## PHASE 4: Attendance and hours approval

### P4a: Attendance backend

```text
Phase 3 is verified. Start Phase 4a only.

Update docs/api-contract.md first, then implement the attendance backend:
- Attendance entity, repository, DTOs, service and controller for `POST /events/{id}/attendance` (NGO owner, batch marking of PRESENT/ABSENT with hours) and `GET /volunteers/me/attendance` (VOLUNTEER).
- Rules from the master prompt: only APPROVED registrations of the NGO's own events; only after the event date has passed or the event is COMPLETED; one record per registration (unique constraint plus service check); hours greater than 0 and not above the scheduled duration plus a small documented tolerance (put the tolerance in a named constant); ABSENT records carry 0 hours; volunteers can never create or edit attendance or hours.
- New records start with hours_status PENDING.
- Use @PreAuthorize plus ownership checks in the service. Avoid N+1 queries (use a fetch join or DTO projection and explain it).
- Add unit tests for the service rules and MockMvc tests for authorization (attendance by an unauthorized user, for a non-approved registration, invalid or excessive hours, volunteer trying to edit hours).

Run the build and tests, start the app, and show real output of each endpoint, including at least three failure cases. Stop after this and wait for me.
```

**Verify:**
- [ ] Marking attendance for a non-approved registration is refused
- [ ] Another NGO cannot mark attendance on this NGO's event
- [ ] Hours of 0, negative or above the duration limit are rejected
- [ ] A volunteer calling the NGO endpoint gets 403
- [ ] Duplicate attendance for the same registration is refused (409)

### P4b: Hours review, audit log, notifications

```text
Phase 4a is verified. Start Phase 4b only.

Update docs/api-contract.md first, then implement:
- `PATCH /attendance/{id}/hours-review` (NGO owner or ADMIN): approve or reject hours, with a mandatory reason on rejection. Approved records are immutable.
- Audit log entries (audit_logs) for hours approval and rejection, written by an AuditLogService reused later by other phases.
- Notifications through the existing NotificationService: "hours approved" and "hours rejected" to the volunteer.
- Make the approval method a clean extension point, because Phase 6 will auto-issue a certificate from it. Do NOT build certificates yet.
- Tests: approved record cannot be changed, rejection without a reason fails, NGO cannot review another NGO's record, admin can review any record, volunteer gets 403, the audit row and notification are created.

Run the build and tests, show real output, and stop.
```

**Verify:**
- [ ] Approving an already-approved record fails
- [ ] Rejection without a reason returns 400 or 422
- [ ] An audit log row exists after each review
- [ ] Volunteer sees the notification in the bell

### P4c: Frontend

```text
Phase 4b is verified. Start Phase 4c only: frontend for attendance and hours.
- `attendance-management.html` plus its page script for NGOs: pick one of my events, see approved volunteers, mark present or absent, enter hours, approve or reject hours (reject asks for a reason in a modal). Ratings will be added in Phase 5, so leave a clearly labelled empty section for it.
- Update `my-events.html` so volunteers see attendance status and verified hours per event.
- Reuse api.js, nav.js and the existing CSS components. Use textContent for user data (no unsafe innerHTML). Add loading states, empty states and confirmation dialogs.
- Give me a manual click-through checklist and stop.
```

**Commit:** `git commit -m "Phase 4: attendance and hours approval"`

---

## PHASE 5: Ratings and performance dashboard

### P5a: Ratings and PerformanceCalculator

```text
Phase 4 is verified. Start Phase 5a only.

Update docs/api-contract.md first, then implement:
- Ratings: `POST /attendance/{id}/rating` (NGO owner) and `GET /volunteers/me/ratings` (VOLUNTEER). Only the owning NGO, only for PRESENT volunteers, one rating per attendance record, scores 1 to 5, optional comment. Enforce with service checks, DB unique constraint and CHECK constraints.
- A single `PerformanceCalculator` class (pure logic, no database access) implementing exactly the formulas in section 5.4 of the master prompt: attendance rate, average rating, hours score, rating score, the 40/30/30 weighted score, and redistribution of the rating weight when the volunteer has no ratings. Badge thresholds must be named constants in one place. Explain the redistribution in code comments.
- Unit tests for PerformanceCalculator including boundary values: score 39 vs 40, 64 vs 65, 84 vs 85; zero events; no ratings; hours above 100.
- Rating tests: rating by a different NGO, duplicate rating, out-of-range score.

Nothing computed may be stored in a column. Run the tests, show real output, and stop.
```

**Verify:**
- [ ] All boundary tests pass
- [ ] Duplicate rating returns 409
- [ ] Rating for an ABSENT volunteer is refused

### P5b: Performance, leaderboard, progress and PDF report

```text
Phase 5a is verified. Start Phase 5b only.

Update docs/api-contract.md first, then implement:
- `GET /volunteers/me/performance` (verified hours, completed events, attendance rate, average rating, score, badge), calculated from database rows through PerformanceCalculator.
- `GET /volunteers/me/performance/progress?granularity=week|month` (hours over time, verified hours only, use an aggregate query or DTO projection).
- `PATCH /volunteers/me/leaderboard-visibility` and `GET /leaderboard` (paginated, top 10 by default). Leaderboard shows only display name, score, badge and verified hours. It must exclude opted-out volunteers and must never expose email, phone or other private data. Explain how you avoid loading every volunteer into memory.
- `GET /volunteers/me/performance/report.pdf` using OpenPDF (add the dependency, explain it): summary stats, badge, recent events, ratings summary. Only the volunteer's own data.
- Tests: leaderboard excludes opted-out volunteers and exposes no private fields; a volunteer cannot read another volunteer's performance; PDF endpoint returns application/pdf.

Run the build and tests, show real output, and stop.
```

### P5c: Frontend

```text
Phase 5b is verified. Start Phase 5c only:
- Rating form inside attendance-management.html (punctuality, teamwork, effort, comment).
- `performance.html`: stat cards, badge, progress chart (week/month toggle) drawn with plain SVG or Canvas in vanilla JavaScript (no chart library), leaderboard opt-in toggle, and a "Download PDF report" button.
- `leaderboard.html` with pagination.
- Reuse shared helpers, sanitize DOM output, add loading and empty states, and give me a manual click-through checklist. Stop.
```

**Commit:** `git commit -m "Phase 5: ratings, performance and leaderboard"`

---

## PHASE 6: Certificate generation

### P6a: Backend (one sub-phase is enough)

```text
Phase 5 is verified. Start Phase 6 (backend first).

Update docs/api-contract.md first, then implement:
- Certificate entity, repository, DTOs and CertificateService.
- AUTO-ISSUE: when hours are APPROVED for a PRESENT volunteer (the extension point built in Phase 4b), issue exactly one certificate in the same transaction. Unique certificate number format `VH-<year>-<6-digit sequence>` (explain how the sequence is generated safely). Idempotent: a duplicate attempt must not create a second certificate (service check plus the unique constraint on attendance_id). No manual "generate" button anywhere.
- Notification "certificate issued" and an audit log entry.
- `GET /volunteers/me/certificates` (VOLUNTEER) and `GET /certificates/{id}/pdf` (owner VOLUNTEER or ADMIN only) generating a PDF with OpenPDF: volunteer name, event name, NGO name, verified hours, issue date, certificate number.
- Tests: approval generates exactly one certificate; duplicate generation attempt does not create another; rejected hours generate none; downloading another volunteer's certificate returns 403; admin can download.

Run the build and tests, show real output, and stop.
```

### P6b: Frontend

```text
Phase 6 backend is verified. Start Phase 6b only: certificates.html listing the volunteer's certificates with a download button, a certificate count on the volunteer dashboard area (or placeholder card if the dashboard is not built yet), and the bell notification link. Use shared helpers, and give me a click-through checklist. Stop.
```

**Commit:** `git commit -m "Phase 6: certificates"`

---

## PHASE 7: Admin panel and dashboards

### P7a: Admin backend and frontend

```text
Phase 6 is verified. Start Phase 7a only.

Update docs/api-contract.md first, then implement admin features:
- `GET /admin/users` (search and pagination), `PATCH /admin/users/{id}/status` (activate, suspend, deactivate; suspended or deactivated users cannot log in; an admin cannot suspend themselves), `GET /admin/events`, `POST /admin/events/{id}/remove` (soft removal with a reason; registered volunteers are notified), `GET /admin/audit-logs`, `GET /admin/stats`.
- Audit log entries for status changes and event removal.
- `admin-dashboard.html` with user management table, event removal with reason modal, audit log view and platform statistics cards.
- Tests: volunteer or NGO calling admin endpoints gets 403, suspended user cannot log in, removed event notifies volunteers.

Run the build and tests, show real output, give me a click-through checklist, and stop.
```

### P7b: Dashboards, reports and reminder scheduler

```text
Phase 7a is verified. Start Phase 7b only.

Update docs/api-contract.md first, then implement:
- `GET /dashboard/volunteer`, `GET /dashboard/ngo`, `GET /dashboard/admin`, role-scoped as described in section 5.7 of the master prompt, plus `GET /reports/hours-by-category` and `GET /reports/activity?granularity=`. Use aggregate queries, not loops over entities (no N+1).
- `volunteer-dashboard.html`, `ngo-dashboard.html` and the remaining admin dashboard charts, using plain SVG, Canvas or CSS charts (no chart library).
- A `@Scheduled` job (daily at 08:00, with the cron value in configuration) that notifies approved volunteers of events happening tomorrow, without creating duplicates when it runs twice. Provide a way to trigger it for testing, and a unit test proving no duplicates.

Run the build and tests, show real output, give me a click-through checklist, and stop.
```

**Commit:** `git commit -m "Phase 7: admin panel, dashboards and reminders"`

---

## PHASE 8: Hardening and delivery

### P8a: Tests and security review

```text
Phase 7 is verified. Start Phase 8a only: testing and security.
1. Run the full test suite and report real results. Compare against the required test list in section 12 of the master prompt and list which tests are missing. Write the missing ones (including the concurrent overbooking test, password reset edge cases, and notification ownership tests). State the expected result for each test.
2. Security review of the whole project: check every endpoint for authentication, role rule and ownership check; confirm CSRF works on all state-changing requests; confirm no entity or password hash is returned by any API; confirm no secret is committed (search the repo and git history); check for string-concatenated SQL and unsafe innerHTML. Produce a table of findings with severity and fix every High or Medium finding. Show real output.
3. Create a Postman collection (`docs/postman/volunteer-hub.postman_collection.json`) covering login, CSRF handling and every module, with a short README on importing it.
Stop after this.
```

### P8b: UX polish, README and documentation

```text
Phase 8a is verified. Start Phase 8b only:
- Responsive and UX polish pass: mobile navbar, consistent spacing, accessible labels, keyboard navigation, loading, empty and error states, expired-session redirect. Do not restructure working code.
- README.md: Windows setup, environment variables, database creation, run, test, default admin credentials and how to change them, troubleshooting.
- docs/: final api-contract.md, er-diagram.md (Mermaid, matches the real schema), viva-questions.md (Java, Spring Boot, Hibernate, JPA, JDBC, MySQL, REST, authentication, relationships, plus our own logic: performance formula, overbooking lock, certificate idempotency, CSRF), project-report-notes.md (abstract, problem statement, objectives, scope, requirements, architecture, use-case, activity and sequence diagrams in Mermaid, module descriptions, real test results, screenshots checklist, conclusion, future enhancements, references).
- Make sure a fresh clone can be set up using only the README. Walk through it and report any gap.
Stop after this.
```

### P8c: Deployment guide

```text
Phase 8b is verified. Start Phase 8c only: write docs/deployment-guide.md covering: static frontend hosting versus Spring Boot hosting (do not assume static hosts can run Java or MySQL), hosted MySQL, environment variables and secret management, Flyway in production, HTTPS, session cookie settings (Secure, SameSite) and CORS if the frontend is split from the backend, logging, backups, and free-tier limits. Add a production profile (`application-prod.properties`) that disables the dev sample-data migration or seeds safely, and explain how the default admin password must be changed. Stop after this.
```

**Commit:** `git commit -m "Phase 8: hardening, tests and documentation"` then tag `v1.0`.

---

## PHASE 9: Optional add-ons (only if time permits)

Only start after Phase 8 is verified, and approve each item separately.

```text
Phase 8 is verified and I approve ONE optional add-on: [choose one: QR check-in/check-out | OpenStreetMap link for event location | email reminders via Spring Mail | event photo gallery | volunteer skill tags with matching | dark mode].
Start it as a self-contained feature branch. First tell me in 10 lines what schema, API and frontend changes it needs and what could break existing features. Update the API contract, implement, add tests, run everything with real output, and stop. Do not touch other add-ons.
```

---

# SECTION D: Recovery prompts

**Build or test failure**
```text
Do not proceed. Find the root cause of this error, fix it, rerun the checks, and show real output: [paste the first error only]
```

**Agent stopped silently**
```text
Continue from where you stopped. Check the repo state first (git status, .\mvnw.cmd clean test), tell me what already exists for the current sub-phase, then finish only that sub-phase. Do not rewrite files that already compile.
```

**Agent drifted from the rules**
```text
Re-read docs/MASTER_PROMPT.md sections 3, 4, 7 and 10. Your last change broke these rules: [state which]. Fix only that and explain what you changed.
```

**Flyway error (checksum or validation)**
```text
Do not edit any migration that has already been applied. Explain the Flyway error, tell me whether the fix is a new migration or a local database reset (give the exact PowerShell and SQL commands), and wait for my approval before running anything destructive.
```

**Before each commit**
```text
Give me the git commands to commit this phase on a feature branch (feature/<module>-<name>), plus a one-paragraph explanation I can use in my viva for what this phase does.
```

---

# SECTION E: Quick tips for Antigravity

1. One sub-phase per conversation (or two small ones). Long threads make the agent forget rules.
2. Always re-attach `MASTER_PROMPT.md` and this file in a new conversation.
3. Commit after every verified sub-phase so a stall never loses work.
4. If a command runs forever (starting the app), tell the agent to run the server in the background and test from a second terminal with `Invoke-RestMethod` or `curl.exe`.
5. Never accept "tests passed" without seeing the actual Maven output.
6. Phase order matters: P4 hours approval is the hook for P6 certificates, and P5 depends on verified attendance data. Do not skip ahead.
