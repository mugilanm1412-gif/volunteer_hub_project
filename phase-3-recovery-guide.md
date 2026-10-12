# Phase 3 Recovery Guide: Volunteer Hub (Antigravity IDE)

Antigravity stalled during Phase 3. This is the largest phase in the master prompt (admin NGO verification, event CRUD, search and filters, registration, overbooking locks, My Events, notifications, and an API contract written first). That is roughly 25 to 30 files across backend and frontend, so the agent often runs out of context or hits a failing build and stops mid-way.

The fix: check what state the repo is in, then run Phase 3 as three smaller sub-phases.

---

## Step 1: Check the repo state

Open the terminal in Antigravity and run:

```powershell
git status
.\mvnw.cmd clean test
```

- **Build fails:** copy the first error only (not the whole log) and send the "If something fails" prompt below.
- **Build passes:** the agent simply ran out of steam. Continue with Step 2.

### If something fails (prompt)

> Do not proceed. Find the root cause of this error, fix it, rerun the checks, and show real output: [paste the first error here]

---

## Step 2: Send Phase 3 as three sub-phases

Send **one prompt at a time**. Verify and commit before sending the next.

### P3a: Events backend

> Phase 2 is verified. Start Phase 3a only. First check the current state of the repo (`git status`, `.\mvnw.cmd clean test`) and tell me what already exists from the previous Phase 3 attempt; do not rewrite files that already compile. Then write the Phase 3 section of `docs/api-contract.md` and implement: admin NGO verification endpoint, the `Event` entity, repository, service, DTOs and controller for NGO event create, edit, delete, cancel and complete, with ownership checks and the verified-NGO rule. Run the build, start the app, and show real output of calling each endpoint. Stop after this and wait for me.

**Verify before moving on:**
- [ ] `.\mvnw.cmd clean test` passes
- [ ] App starts and connects to MySQL
- [ ] Unverified NGO cannot create an event (expect 403)
- [ ] NGO cannot edit another NGO's event (expect 403 or 404)
- [ ] Delete with registrations is refused; cancel works

```powershell
git add .
git commit -m "Phase 3a: events backend and NGO verification"
```

### P3b: Registration and overbooking

> Phase 3a is verified. Start Phase 3b only: volunteer browse with search and filters (pagination), event details, register and cancel, NGO approve or reject registrations, pessimistic write lock against overbooking, and notifications for approvals. Include a test that proves overbooking is prevented. Run everything and show real output. Stop after this.

**Verify before moving on:**
- [ ] Browse, search by `q`, `category`, `location`, `from`, `to`, `hasSlots` all work
- [ ] Duplicate registration returns 409
- [ ] Registering for a full, cancelled or past event is refused
- [ ] Concurrent overbooking test passes (approved count never exceeds capacity)
- [ ] Approved and rejected notifications appear for the volunteer

```powershell
git add .
git commit -m "Phase 3b: registrations, filters and overbooking protection"
```

### P3c: Frontend pages

> Phase 3b is verified. Start Phase 3c only: `events.html`, `event-details.html`, `create-event.html`, `my-events.html`, a minimal admin NGO-verification page, and their page scripts, using the existing `api.js` and `nav.js`. Give me a manual click-through checklist. Stop after this.

**Verify before moving on:**
- [ ] Admin can verify the pending NGO from the browser
- [ ] Verified NGO can create, edit and cancel an event
- [ ] Volunteer can browse, filter, view details, register and cancel
- [ ] NGO can approve or reject registrations
- [ ] My Events tabs show upcoming, completed and pending correctly
- [ ] Expired session redirects to login

```powershell
git add .
git commit -m "Phase 3c: events and registration frontend"
```

---

## Tips to reduce stalling in Antigravity

1. **Start a fresh conversation for each sub-phase** and attach the master prompt file again. Long threads degrade.
2. **Commit after every sub-phase** so a stall never loses work.
3. **Long-running commands:** if it halts while starting the app, tell it to run the server in the background and test with `Invoke-RestMethod` or `curl.exe` in a second terminal.
4. **Silent stop, no error:** send this:
   > Continue from where you stopped. Check the repo state first (`git status`, `.\mvnw.cmd clean test`), then finish only the current sub-phase.
5. **Do not let it rewrite working code.** Remind it of master prompt rule 6: fix only what is broken.

---

## After Phase 3 is done

Use the standard command from the appendix for the next phases:

> Phase 3 is verified. Start Phase 4 exactly as specified in the master prompt. Write or update the API contract first.

If Phase 4 also stalls, split it the same way: 4a (attendance and hours backend), 4b (approval, audit log, notifications), 4c (frontend).

Before each commit, you can also ask:

> Give me the git commands to commit this phase on a feature branch, plus a one-paragraph explanation I can use in my viva for what this phase does.
