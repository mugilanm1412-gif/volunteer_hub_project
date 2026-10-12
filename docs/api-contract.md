# Volunteer Hub: REST API Contract

All endpoints are prefixed with `/api` and exchange data in UTF-8 JSON. Authentication uses standard session cookies (`JSESSIONID`) with CSRF protection (`X-XSRF-TOKEN`).

---

## 1. Global Error Response Specification

When an error occurs, all endpoints return a consistent JSON payload:

```json
{
  "timestamp": "2026-10-12T08:15:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for request parameters",
  "fieldErrors": {
    "email": "Email must be a valid email address",
    "password": "Password must be at least 8 characters long"
  }
}
```

---

## 2. Authentication & Account Management (`/api/auth`, `/api/users`)

### 2.1 Register as Volunteer
- **Method:** `POST`
- **URL:** `/api/auth/register/volunteer`
- **Required Role:** Public (Unauthenticated)
- **Request Body:**
  ```json
  {
    "email": "jane@example.com",
    "password": "Password@123",
    "fullName": "Jane Doe",
    "phone": "+1-555-0301",
    "city": "Eco City",
    "skills": "First aid, Cooking",
    "interests": "Environment, Community Aid",
    "bio": "Excited to contribute to community welfare.",
    "showOnLeaderboard": true
  }
  ```
- **Validation Rules:**
  - `email`: Not blank, valid email format, unique in database.
  - `password`: Minimum 8 characters.
  - `fullName`: Not blank, max 150 characters.
  - `phone`: Optional, max 20 characters.
  - `city`: Optional, max 100 characters.
- **Success Status:** `201 Created`
- **Response Body:**
  ```json
  {
    "id": 8,
    "email": "jane@example.com",
    "role": "VOLUNTEER",
    "message": "Registration successful. Please log in."
  }
  ```
- **Error Responses:** `400 Bad Request` (validation failure), `409 Conflict` (email already exists).

---

### 2.2 Register as NGO
- **Method:** `POST`
- **URL:** `/api/auth/register/ngo`
- **Required Role:** Public (Unauthenticated)
- **Request Body:**
  ```json
  {
    "email": "hope@ngo.org",
    "password": "Password@123",
    "name": "Hope Foundation",
    "cause": "Education",
    "description": "Providing quality educational resources to underserved youth.",
    "registrationNumber": "NGO-2026-EDU-09",
    "contactPerson": "Sarah Jenkins",
    "contactPhone": "+1-555-0302",
    "address": "77 Hope Lane, Education District"
  }
  ```
- **Validation Rules:**
  - `email`: Not blank, valid email format, unique.
  - `password`: Minimum 8 characters.
  - `name`: Not blank, max 200 characters.
  - `cause`: Not blank, max 100 characters.
  - `registrationNumber`: Not blank, max 100 characters.
  - `contactPerson`: Not blank, max 150 characters.
  - `contactPhone`: Not blank, max 20 characters.
  - `address`: Not blank, max 255 characters.
- **Success Status:** `201 Created`
- **Response Body:**
  ```json
  {
    "id": 9,
    "email": "hope@ngo.org",
    "role": "NGO",
    "message": "Registration successful. Your account is pending administrator verification before you can publish events."
  }
  ```
- **Error Responses:** `400 Bad Request`, `409 Conflict`.

---

### 2.3 User Login
- **Method:** `POST`
- **URL:** `/api/auth/login`
- **Required Role:** Public
- **Request Body:**
  ```json
  {
    "email": "alice@example.com",
    "password": "password123"
  }
  ```
- **Success Status:** `200 OK`
- **Response Cookie:** `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Strict`
- **Response Body:**
  ```json
  {
    "id": 4,
    "email": "alice@example.com",
    "role": "VOLUNTEER",
    "status": "ACTIVE",
    "displayName": "Alice Smith"
  }
  ```
- **Error Responses:** `401 Unauthorized` (invalid credentials or suspended account).

---

### 2.4 User Logout
- **Method:** `POST`
- **URL:** `/api/auth/logout`
- **Required Role:** Public / Any logged-in user
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "message": "Logged out successfully"
  }
  ```

---

### 2.5 Current Authenticated User (`/api/users/me`)
- **Method:** `GET`
- **URL:** `/api/users/me`
- **Required Role:** Any logged-in user (`VOLUNTEER`, `NGO`, `ADMIN`)
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 4,
    "email": "alice@example.com",
    "role": "VOLUNTEER",
    "status": "ACTIVE",
    "displayName": "Alice Smith",
    "verificationStatus": null
  }
  ```
- **Error Responses:** `401 Unauthorized` (JSON format).

---

### 2.6 Change Password (Logged In)
- **Method:** `POST`
- **URL:** `/api/users/me/password`
- **Required Role:** Any logged-in user
- **Request Body:**
  ```json
  {
    "currentPassword": "password123",
    "newPassword": "NewPassword@2026"
  }
  ```
- **Validation Rules:** `currentPassword` not blank; `newPassword` minimum 8 characters.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "message": "Password changed successfully"
  }
  ```
- **Error Responses:** `400 Bad Request` (incorrect current password or weak new password).

---

### 2.7 Forgot Password (Request Reset)
- **Method:** `POST`
- **URL:** `/api/auth/forgot-password`
- **Required Role:** Public
- **Request Body:**
  ```json
  {
    "email": "alice@example.com"
  }
  ```
- **Success Status:** `200 OK`
- **Response Body (Security Rule: Generic response regardless of whether email exists):**
  ```json
  {
    "message": "If this email is registered in our platform, a password reset link has been dispatched."
  }
  ```

---

### 2.8 Reset Password (Using Token)
- **Method:** `POST`
- **URL:** `/api/auth/reset-password`
- **Required Role:** Public
- **Request Body:**
  ```json
  {
    "token": "4a7b98f2e1c3...",
    "newPassword": "NewPassword@2026"
  }
  ```
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "message": "Your password has been successfully reset. You may now log in."
  }
  ```
- **Error Responses:** `400 Bad Request` (token invalid, already used, expired, or weak password).

---

## 3. Profiles (`/api/volunteers/me/profile`, `/api/ngos/me/profile`)

### 3.1 Get Volunteer Profile
- **Method:** `GET`
- **URL:** `/api/volunteers/me/profile`
- **Required Role:** `VOLUNTEER`
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 1,
    "userId": 4,
    "email": "alice@example.com",
    "fullName": "Alice Smith",
    "phone": "+1-555-0201",
    "city": "Eco City",
    "skills": "Tree planting, Team leadership, First aid",
    "interests": "Environment, Animal Welfare",
    "bio": "Passionate environmentalist and weekend tree planter.",
    "showOnLeaderboard": true,
    "createdAt": "2026-10-12T07:40:00Z"
  }
  ```

### 3.2 Update Volunteer Profile
- **Method:** `PUT`
- **URL:** `/api/volunteers/me/profile`
- **Required Role:** `VOLUNTEER`
- **Request Body:**
  ```json
  {
    "fullName": "Alice M. Smith",
    "phone": "+1-555-9999",
    "city": "Green Valley",
    "skills": "Tree planting, First aid, Project management",
    "interests": "Environment, Renewable energy",
    "bio": "Updated volunteer bio.",
    "showOnLeaderboard": true
  }
  ```
- **Success Status:** `200 OK`

---

### 3.3 Get NGO Profile
- **Method:** `GET`
- **URL:** `/api/ngos/me/profile`
- **Required Role:** `NGO`
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 1,
    "userId": 2,
    "email": "contact@greenearth.org",
    "name": "Green Earth Foundation",
    "cause": "Environment",
    "description": "Dedicated to urban reforestation...",
    "registrationNumber": "NGO-REG-2024-001",
    "contactPerson": "Elena Vance",
    "contactPhone": "+1-555-0101",
    "address": "124 Green Boulevard, Eco City",
    "verificationStatus": "VERIFIED",
    "rejectionReason": null
  }
  ```

### 3.4 Update NGO Profile
- **Method:** `PUT`
- **URL:** `/api/ngos/me/profile`
- **Required Role:** `NGO`
- **Request Body:**
  ```json
  {
    "name": "Green Earth Foundation",
    "cause": "Environment & Sustainability",
    "description": "Expanded mission...",
    "contactPerson": "Elena Vance",
    "contactPhone": "+1-555-0101",
    "address": "126 Green Boulevard, Eco City"
  }
  ```
- **Success Status:** `200 OK`

---

## 4. Notifications (`/api/notifications`)

### 4.1 List My Notifications (Paginated)
- **Method:** `GET`
- **URL:** `/api/notifications?page=0&size=10`
- **Required Role:** Any logged-in user
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "content": [
      {
        "id": 3,
        "type": "CERTIFICATE_ISSUED",
        "title": "Certificate Issued",
        "message": "Your certificate VH-2026-000001 is ready for download.",
        "read": false,
        "relatedEntityType": "CERTIFICATE",
        "relatedEntityId": 1,
        "createdAt": "2026-10-12T07:40:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

### 4.2 Get Unread Notification Count
- **Method:** `GET`
- **URL:** `/api/notifications/unread-count`
- **Required Role:** Any logged-in user
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "unreadCount": 1
  }
  ```

### 4.3 Mark Notification as Read
- **Method:** `PATCH`
- **URL:** `/api/notifications/{id}/read`
- **Required Role:** Notification Owner
- **Success Status:** `200 OK`

### 4.4 Mark All Notifications as Read
- **Method:** `PATCH`
- **URL:** `/api/notifications/read-all`
- **Required Role:** Notification Owner
- **Success Status:** `200 OK`

---

## 5. Admin NGO Verification (`/api/admin/ngos`)

### 5.1 Verify or Reject NGO
- **Method:** `PATCH`
- **URL:** `/api/admin/ngos/{id}/verification`
- **Required Role:** `ADMIN`
- **Request Body:**
  ```json
  {
    "status": "VERIFIED",
    "rejectionReason": null
  }
  ```
- **Validation Rules:**
  - `status`: Must be either `VERIFIED` or `REJECTED`.
  - `rejectionReason`: Required if status is `REJECTED`.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 2,
    "ngoName": "Ocean Rescue Initiative",
    "verificationStatus": "VERIFIED",
    "message": "NGO verification status updated successfully."
  }
  ```
- **Error Responses:** `400 Bad Request`, `404 Not Found`.

### 5.2 List NGOs
- **Method:** `GET`
- **URL:** `/api/admin/ngos?status=PENDING`
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body:** Array of `NgoProfileDto` objects.

---

## 6. Events Management (`/api/events`)

### 6.1 List Published Events (Browse / Search / Filter)
- **Method:** `GET`
- **URL:** `/api/events?q=beach&category=Environment&location=Eco City&from=2026-10-01&to=2026-12-31&hasSlots=true&page=0&size=10`
- **Required Role:** Public / Authenticated
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "content": [
      {
        "id": 2,
        "ngoId": 2,
        "ngoName": "Green Earth Foundation",
        "title": "Urban Reforestation & Sapling Care",
        "description": "Planting 300 native tree saplings...",
        "category": "Environment",
        "location": "Metro Green Belt Park",
        "eventDate": "2026-10-19",
        "startTime": "09:00:00",
        "endTime": "13:00:00",
        "requiredSkills": "Gardening, Enthusiasm",
        "capacity": 15,
        "approvedCount": 1,
        "remainingSlots": 14,
        "status": "PUBLISHED"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

### 6.2 Get Event Details
- **Method:** `GET`
- **URL:** `/api/events/{id}`
- **Required Role:** Public / Authenticated (Enriches with volunteer's application status if logged in)
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 2,
    "ngoId": 2,
    "ngoName": "Green Earth Foundation",
    "ngoContactPerson": "Elena Vance",
    "ngoContactPhone": "+1-555-0101",
    "title": "Urban Reforestation & Sapling Care",
    "description": "Planting 300 native tree saplings...",
    "category": "Environment",
    "location": "Metro Green Belt Park",
    "eventDate": "2026-10-19",
    "startTime": "09:00:00",
    "endTime": "13:00:00",
    "requiredSkills": "Gardening, Enthusiasm",
    "capacity": 15,
    "approvedCount": 1,
    "remainingSlots": 14,
    "status": "PUBLISHED",
    "userApplicationStatus": "APPROVED"
  }
  ```
- **Error Responses:** `404 Not Found`.

### 6.3 Create Event
- **Method:** `POST`
- **URL:** `/api/events`
- **Required Role:** `NGO` (Must be `VERIFIED`)
- **Request Body:**
  ```json
  {
    "title": "Riverside Cleanup Drive",
    "description": "Cleaning up debris along the East River canal.",
    "category": "Environment",
    "location": "River Walk, Sector 4",
    "eventDate": "2026-11-05",
    "startTime": "08:00:00",
    "endTime": "12:00:00",
    "requiredSkills": "Teamwork, Outdoor endurance",
    "capacity": 30,
    "status": "PUBLISHED"
  }
  ```
- **Validation Rules:**
  - `title`: Not blank, max 200 characters.
  - `description`: Not blank.
  - `category`: Not blank, max 100 characters.
  - `location`: Not blank, max 255 characters.
  - `eventDate`: Present or future date.
  - `startTime`, `endTime`: Valid time format, `endTime` after `startTime`.
  - `capacity`: Integer >= 1.
  - `status`: `DRAFT` or `PUBLISHED`.
- **Success Status:** `201 Created`
- **Response Body:** `EventDto`
- **Error Responses:** `400 Bad Request`, `403 Forbidden` (if NGO is not VERIFIED).

### 6.4 Update Event
- **Method:** `PUT`
- **URL:** `/api/events/{id}`
- **Required Role:** `NGO` (Owner of the event)
- **Request Body:** Same as Create Event
- **Success Status:** `200 OK`
- **Response Body:** `EventDto`
- **Error Responses:** `400 Bad Request`, `403 Forbidden`, `404 Not Found`.

### 6.5 Delete Event
- **Method:** `DELETE`
- **URL:** `/api/events/{id}`
- **Required Role:** `NGO` (Owner of the event)
- **Rule:** Permitted only if the event has **zero registrations**. Otherwise, the event must be cancelled.
- **Success Status:** `204 No Content`
- **Error Responses:** `400 Bad Request` (if registrations exist), `403 Forbidden`, `404 Not Found`.

### 6.6 Cancel Event
- **Method:** `POST`
- **URL:** `/api/events/{id}/cancel`
- **Required Role:** `NGO` (Owner of the event)
- **Request Body:**
  ```json
  {
    "reason": "Severe weather alert predicted on event day."
  }
  ```
- **Rule:** Marks event as `CANCELLED` and dispatches in-app notifications to all registered volunteers.
- **Success Status:** `200 OK`
- **Error Responses:** `400 Bad Request`, `403 Forbidden`, `404 Not Found`.

### 6.7 Complete Event
- **Method:** `POST`
- **URL:** `/api/events/{id}/complete`
- **Required Role:** `NGO` (Owner of the event)
- **Rule:** Marks event as `COMPLETED`. Enables attendance marking in Phase 4.
- **Success Status:** `200 OK`
- **Error Responses:** `400 Bad Request`, `403 Forbidden`, `404 Not Found`.

### 6.8 List NGO's Own Events
- **Method:** `GET`
- **URL:** `/api/events/mine?page=0&size=10`
- **Required Role:** `NGO`
- **Success Status:** `200 OK`
- **Response Body:** Paginated list of `EventDto` objects.

---

## 7. Event Applications / Registrations (`/api/events/{id}/applications`, `/api/applications`, `/api/volunteers/me/applications`)

### 7.1 Register for Event (Volunteer Application)
- **Method:** `POST`
- **URL:** `/api/events/{id}/applications`
- **Required Role:** `VOLUNTEER`
- **Rule:** Pessimistic lock (`@Lock(PESSIMISTIC_WRITE)`) on the event row prevents overbooking. Cannot register if event is cancelled, completed, removed, in the past, or full. Duplicate registration prohibited.
- **Success Status:** `201 Created`
- **Response Body:**
  ```json
  {
    "id": 7,
    "eventId": 2,
    "eventTitle": "Urban Reforestation & Sapling Care",
    "volunteerId": 4,
    "status": "PENDING",
    "appliedAt": "2026-10-12T14:40:00Z",
    "message": "Application submitted successfully. Awaiting NGO review."
  }
  ```
- **Error Responses:** `400 Bad Request` (event full, past, or cancelled), `409 Conflict` (already registered).

### 7.2 Get Event Applications (NGO Review)
- **Method:** `GET`
- **URL:** `/api/events/{id}/applications`
- **Required Role:** `NGO` (Owner of the event)
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  [
    {
      "id": 5,
      "volunteerId": 7,
      "volunteerName": "Diana Prince",
      "volunteerEmail": "diana@example.com",
      "volunteerPhone": "+1-555-0204",
      "volunteerSkills": "Teaching, Coordination, Art therapy",
      "status": "PENDING",
      "appliedAt": "2026-10-11T12:00:00Z"
    }
  ]
  ```

### 7.3 Approve or Reject Application
- **Method:** `PATCH`
- **URL:** `/api/applications/{id}/status`
- **Required Role:** `NGO` (Owner of the event)
- **Request Body:**
  ```json
  {
    "status": "APPROVED"
  }
  ```
- **Rule:** When approving, locks event row to ensure `approvedCount < capacity`. On approval or rejection, sends in-app notification to the volunteer.
- **Success Status:** `200 OK`
- **Response Body:** `ApplicationDto`
- **Error Responses:** `400 Bad Request` (capacity reached or invalid transition), `403 Forbidden`.

### 7.4 Cancel Application (Volunteer Self-Cancellation)
- **Method:** `POST`
- **URL:** `/api/applications/{id}/cancel`
- **Required Role:** `VOLUNTEER` (Owner of the application)
- **Rule:** Can only cancel before event date. If application was `APPROVED`, cancelling frees the reserved slot.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "id": 5,
    "status": "CANCELLED",
    "message": "Registration cancelled successfully."
  }
  ```
- **Error Responses:** `400 Bad Request` (event date has already arrived or passed), `403 Forbidden`.

### 7.5 List Volunteer's Applications (My Events)
- **Method:** `GET`
- **URL:** `/api/volunteers/me/applications?tab=upcoming&page=0&size=10`
- **Required Role:** `VOLUNTEER`
- **Parameters:**
  - `tab`: `upcoming` (Approved & event date >= today), `completed` (Approved & event date < today or event COMPLETED), `pending` (Status PENDING), or `all`
  - `page`, `size`
- **Success Status:** `200 OK`
- **Response Body:** Paginated list of `VolunteerApplicationDto`

---

## 8. Attendance Management (`/api/events/{id}/attendance`, `/api/volunteers/me/attendance`)

### 8.1 Batch Mark Attendance
- **Method:** `POST`
- **URL:** `/api/events/{id}/attendance`
- **Required Role:** `NGO` (Owner of the event)
- **Rules:**
  - The event must have passed (`eventDate < CURRENT_DATE`) or have status `COMPLETED`.
  - Only registrations with status `APPROVED` are eligible.
  - One attendance record per registration (enforced by DB unique constraint on `application_id` and service pre-check).
  - For `ABSENT` records, `hoursWorked` is strictly `0.0`.
  - For `PRESENT` records, `hoursWorked` must be `> 0` and cannot exceed the scheduled duration plus the documented tolerance of 1.0 hour (`ATTENDANCE_HOURS_TOLERANCE`).
  - New records are initialized with `hoursStatus = PENDING`.
  - Volunteers attempting to call this endpoint receive `403 Forbidden`.
- **Request Body:**
  ```json
  {
    "records": [
      {
        "applicationId": 5,
        "attendanceStatus": "PRESENT",
        "hoursWorked": 4.0
      },
      {
        "applicationId": 6,
        "attendanceStatus": "ABSENT",
        "hoursWorked": 0.0
      }
    ]
  }
  ```
- **Success Status:** `201 Created`
- **Response Body:**
  ```json
  [
    {
      "id": 1,
      "applicationId": 5,
      "volunteerId": 7,
      "volunteerName": "Diana Prince",
      "eventId": 2,
      "eventTitle": "Urban Reforestation & Sapling Care",
      "attendanceStatus": "PRESENT",
      "hoursWorked": 4.0,
      "hoursStatus": "PENDING",
      "markedAt": "2026-10-12T18:00:00Z",
      "rejectReason": null
    },
    {
      "id": 2,
      "applicationId": 6,
      "volunteerId": 8,
      "volunteerName": "Bruce Wayne",
      "eventId": 2,
      "eventTitle": "Urban Reforestation & Sapling Care",
      "attendanceStatus": "ABSENT",
      "hoursWorked": 0.0,
      "hoursStatus": "PENDING",
      "markedAt": "2026-10-12T18:00:00Z",
      "rejectReason": null
    }
  ]
  ```
- **Error Responses:**
  - `400 Bad Request`: Event not concluded yet; application not in `APPROVED` state; hours `<= 0` or exceeding duration limit.
  - `403 Forbidden`: Authenticated user is not the owner NGO.
  - `404 Not Found`: Event or application does not exist.
  - `409 Conflict`: Attendance record already exists for the registration.

### 8.2 Get Volunteer My Attendance
- **Method:** `GET`
- **URL:** `/api/volunteers/me/attendance`
- **Required Role:** `VOLUNTEER`
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  [
    {
      "id": 1,
      "applicationId": 5,
      "eventId": 2,
      "eventTitle": "Urban Reforestation & Sapling Care",
      "ngoName": "Green Earth Foundation",
      "eventDate": "2026-10-11",
      "attendanceStatus": "PRESENT",
      "hoursWorked": 4.0,
      "hoursStatus": "PENDING",
      "reviewedAt": null,
      "rejectReason": null
    }
  ]
  ```

### 8.3 Review Volunteer Hours (Approve or Reject)
- **Method:** `PATCH`
- **URL:** `/api/attendance/{id}/hours-review`
- **Required Role:** `NGO` (Owner of the event) or `ADMIN`
- **Rules:**
  - An already `APPROVED` attendance record is immutable and cannot be re-reviewed.
  - When status is `REJECTED`, the `reason` field is mandatory (not blank).
  - Admin users can review any attendance record; NGOs can only review records from their own events.
  - Volunteers attempting to call this endpoint receive `403 Forbidden`.
  - When hours are approved or rejected, an audit log entry is written to `audit_logs` and an in-app notification (`HOURS_APPROVED` or `HOURS_REJECTED`) is delivered to the volunteer.
- **Request Body:**
  ```json
  {
    "status": "APPROVED",
    "reason": null
  }
  ```
  or on rejection:
  ```json
  {
    "status": "REJECTED",
    "reason": "Volunteer was absent for the afternoon shift."
  }
  ```
- **Success Status:** `200 OK`
- **Response Body:** `AttendanceResponseDto`
- **Error Responses:**
  - `400 Bad Request`: Record is already approved (immutable) or rejection reason is missing.
  - `403 Forbidden`: Authenticated NGO does not own the event associated with this attendance record.
  - `404 Not Found`: Attendance record does not exist.

---

## 9. Volunteer Ratings (`/api/attendance/{id}/rating`, `/api/volunteers/me/ratings`)

### 9.1 Rate Volunteer Attendance Shift
- **Method:** `POST`
- **URL:** `/api/attendance/{id}/rating`
- **Required Role:** `NGO` (Owner of the event)
- **Rules:**
  - Only the owning NGO of the event hosting the attendance record can submit a rating.
  - Ratings can only be submitted for volunteers marked `PRESENT` (refused with `400 Bad Request` if `ABSENT`).
  - Exactly one rating per attendance record (enforced by DB unique constraint on `attendance_id` and service check; duplicate submission returns `409 Conflict`).
  - Ratings require integer scores between 1 and 5 for:
    - `punctuality`: 1 to 5
    - `teamwork`: 1 to 5
    - `effort`: 1 to 5
  - `comment` is optional (up to 1000 characters).
  - On rating creation, an in-app notification (`RATING_RECEIVED`) is delivered to the volunteer.
- **Request Body:**
  ```json
  {
    "punctuality": 5,
    "teamwork": 4,
    "effort": 5,
    "comment": "Outstanding initiative and team leadership throughout the cleanup drive."
  }
  ```
- **Success Status:** `201 Created`
- **Response Body:**
  ```json
  {
    "id": 1,
    "attendanceId": 12,
    "eventId": 2,
    "eventTitle": "Urban Reforestation & Sapling Care",
    "ngoName": "Green Earth Foundation",
    "punctuality": 5,
    "teamwork": 4,
    "effort": 5,
    "averageScore": 4.67,
    "comment": "Outstanding initiative and team leadership throughout the cleanup drive.",
    "createdAt": "2026-10-12T19:00:00Z"
  }
  ```
- **Error Responses:**
  - `400 Bad Request`: Score values outside 1–5 range, or volunteer was marked `ABSENT`.
  - `403 Forbidden`: Authenticated user is not the owning NGO.
  - `404 Not Found`: Attendance record does not exist.
  - `409 Conflict`: Rating already exists for this attendance record.

### 9.2 Get Volunteer Ratings History
- **Method:** `GET`
- **URL:** `/api/volunteers/me/ratings`
- **Required Role:** `VOLUNTEER`
- **Rules:**
  - Returns all performance evaluations and feedback comments received by the authenticated volunteer.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  [
    {
      "id": 1,
      "attendanceId": 12,
      "eventId": 2,
      "eventTitle": "Urban Reforestation & Sapling Care",
      "ngoName": "Green Earth Foundation",
      "punctuality": 5,
      "teamwork": 4,
      "effort": 5,
      "averageScore": 4.67,
      "comment": "Outstanding initiative and team leadership throughout the cleanup drive.",
      "createdAt": "2026-10-12T19:00:00Z"
    }
  ]
  ```

---

## 10. Performance Dashboard, Leaderboard & Reports (`/api/volunteers/me/performance`, `/api/leaderboard`)

### 10.1 Get Volunteer Performance Summary
- **Method:** `GET`
- **URL:** `/api/volunteers/me/performance`
- **Required Role:** `VOLUNTEER`
- **Rules:**
  - Aggregates verified hours, attendance records, and evaluations dynamically at query time using `PerformanceCalculator`.
  - Zero computed data is stored in the database.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "totalVerifiedHours": 44.0,
    "completedEvents": 8,
    "registeredCompletedEvents": 10,
    "attendanceRate": 80.0,
    "averageRating": 4.67,
    "hoursScore": 44.0,
    "attendanceScore": 80.0,
    "ratingScore": 93.4,
    "performanceScore": 69.62,
    "badge": "SILVER",
    "showOnLeaderboard": true
  }
  ```

### 10.2 Get Performance Progress Over Time
- **Method:** `GET`
- **URL:** `/api/volunteers/me/performance/progress?granularity=week|month`
- **Required Role:** `VOLUNTEER`
- **Parameters:**
  - `granularity`: `week` or `month` (default: `month`)
- **Rules:**
  - Returns chronological time series of approved, verified service hours for the volunteer.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  [
    {
      "period": "2026-09",
      "hours": 16.0
    },
    {
      "period": "2026-10",
      "hours": 28.0
    }
  ]
  ```

### 10.3 Update Leaderboard Visibility
- **Method:** `PATCH`
- **URL:** `/api/volunteers/me/leaderboard-visibility`
- **Required Role:** `VOLUNTEER`
- **Request Body:**
  ```json
  {
    "showOnLeaderboard": false
  }
  ```
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "showOnLeaderboard": false,
    "message": "Leaderboard visibility preference updated successfully."
  }
  ```

### 10.4 Get Public Leaderboard
- **Method:** `GET`
- **URL:** `/api/leaderboard?page=0&size=10`
- **Required Role:** Public / Authenticated
- **Rules:**
  - Ranks volunteers by composite performance score descending, then verified hours descending.
  - Strictly filters out volunteers with `showOnLeaderboard = false` or inactive user accounts.
  - Never exposes email, phone number, address, or other sensitive volunteer information.
  - Only returns `rank`, `displayName`, `score`, `badge`, and `verifiedHours`.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "content": [
      {
        "rank": 1,
        "displayName": "Alice Smith",
        "score": 88.5,
        "badge": "GOLD",
        "verifiedHours": 54.0
      },
      {
        "rank": 2,
        "displayName": "Bob Johnson",
        "score": 72.0,
        "badge": "SILVER",
        "verifiedHours": 24.0
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "last": true
  }
  ```

### 10.5 Download Volunteer Performance PDF Report
- **Method:** `GET`
- **URL:** `/api/volunteers/me/performance/report.pdf`
- **Required Role:** `VOLUNTEER`
- **Rules:**
  - Generates an official, styled performance report document via OpenPDF.
  - Restricted strictly to the authenticated volunteer's own verified data.
- **Success Status:** `200 OK`
- **Response Headers:**
  - `Content-Type: application/pdf`
  - `Content-Disposition: attachment; filename="volunteer-performance-report.pdf"`

---

## 11. Certificates (`/api/volunteers/me/certificates`, `/api/certificates/{id}/pdf`)

### 11.1 List Volunteer Certificates
- **Method:** `GET`
- **URL:** `/api/volunteers/me/certificates`
- **Required Role:** `VOLUNTEER`
- **Rules:**
  - Retrieves all certificates issued to the authenticated volunteer.
  - Certificates are automatically created when attendance hours are approved (`APPROVED`) for a `PRESENT` shift.
  - Each certificate contains a unique certificate number formatted as `VH-<year>-<6-digit sequence>` (e.g. `VH-2026-000001`).
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  [
    {
      "id": 1,
      "attendanceId": 12,
      "certificateNumber": "VH-2026-000001",
      "issuedOn": "2026-10-12T14:30:00",
      "eventId": 3,
      "eventTitle": "Urban Reforestation & Sapling Care",
      "ngoName": "Green Earth Foundation",
      "volunteerName": "Alice Smith",
      "hoursWorked": 4.0,
      "downloadUrl": "/api/certificates/1/pdf"
    }
  ]
  ```

### 11.2 Download Certificate PDF
- **Method:** `GET`
- **URL:** `/api/certificates/{id}/pdf`
- **Required Role:** Owner `VOLUNTEER` or `ADMIN`
- **Rules:**
  - Generates an official, verifiable Certificate of Volunteer Service document using OpenPDF.
  - Documents volunteer name, event title, host NGO, verified hours, issue date, unique certificate number, and electronic verification seal.
  - Restricted to the volunteer who earned the certificate or system administrators (`ADMIN`).
  - Attempt by another volunteer to download returns `403 Forbidden`.
  - Non-existent certificate returns `404 Not Found`.
- **Success Status:** `200 OK`
- **Response Headers:**
  - `Content-Type: application/pdf`
  - `Content-Disposition: attachment; filename="certificate-VH-2026-000001.pdf"`

---

## 12. Admin Governance & Platform Management (`/api/admin/**`)

### 12.1 List & Search Platform Users
- **Method:** `GET`
- **URL:** `/api/admin/users?query={string}&role={Role}&status={UserStatus}&page={int}&size={int}`
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "content": [
      {
        "id": 1,
        "email": "alice@example.com",
        "displayName": "Alice Smith",
        "role": "VOLUNTEER",
        "status": "ACTIVE",
        "createdAt": "2026-10-10T12:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
  ```

### 12.2 Update User Account Lifecycle Status
- **Method:** `PATCH`
- **URL:** `/api/admin/users/{id}/status`
- **Required Role:** `ADMIN`
- **Request Body:**
  ```json
  {
    "status": "SUSPENDED"
  }
  ```
- **Rules:**
  - Status must be one of `ACTIVE`, `SUSPENDED`, `DEACTIVATED`.
  - An administrator cannot suspend or deactivate their own account (`400 Bad Request`).
  - Suspended or deactivated users are rejected during authentication attempts.
  - Automatically writes an immutable entry to the platform `audit_logs` table.
- **Success Status:** `200 OK`
- **Response Body:** Updated `AdminUserDto`.

### 12.3 List Platform Events for Governance
- **Method:** `GET`
- **URL:** `/api/admin/events?query={string}&status={EventStatus}&page={int}&size={int}`
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body:** Paginated `PageResponseDto<AdminEventItemDto>`.

### 12.4 Soft-Remove Inappropriate Event
- **Method:** `POST`
- **URL:** `/api/admin/events/{id}/remove`
- **Required Role:** `ADMIN`
- **Request Body:**
  ```json
  {
    "reason": "Violation of community standards and misleading schedule details."
  }
  ```
- **Rules:**
  - Soft removal sets event status to `REMOVED` and persists `removed_reason`.
  - All registered volunteers with `APPROVED` or `PENDING` applications receive an in-app `EVENT_REMOVED` notification.
  - The organizing NGO is also notified in-app.
  - Creates an audit log entry (`EVENT_REMOVED`).
- **Success Status:** `200 OK`

### 12.5 Retrieve Platform Audit Logs
- **Method:** `GET`
- **URL:** `/api/admin/audit-logs?action={string}&entityType={string}&page={int}&size={int}`
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body:** Paginated `PageResponseDto<AuditLogDto>`.

### 12.6 Platform Summary Statistics
- **Method:** `GET`
- **URL:** `/api/admin/stats`
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "totalUsers": 25,
    "totalVolunteers": 18,
    "totalNgos": 6,
    "activeUsers": 24,
    "suspendedUsers": 1,
    "totalEvents": 14,
    "publishedEvents": 8,
    "completedEvents": 5,
    "removedEvents": 1,
    "totalRegistrations": 42,
    "totalVerifiedHours": 128.5,
    "totalCertificatesIssued": 19,
    "pendingNgoVerifications": 1
  }
  ```

---

## 13. Dashboards, Reports & Reminder Scheduler (`/api/dashboard/*`, `/api/reports/*`)

### 13.1 Volunteer Dashboard
- **Method:** `GET`
- **URL:** `/api/dashboard/volunteer` (also accessible via `/dashboard/volunteer`)
- **Required Role:** `VOLUNTEER`
- **Success Status:** `200 OK`
- **Response Body (`VolunteerDashboardDto`):**
  ```json
  {
    "volunteerName": "Alice Smith",
    "verifiedHours": 24.50,
    "completedEvents": 5,
    "upcomingApprovedEvents": [
      {
        "id": 12,
        "title": "Community Park Clean-Up",
        "category": "Environment",
        "eventDate": "2026-10-15",
        "startTime": "09:00:00",
        "endTime": "13:00:00",
        "location": "Central Green Park",
        "ngoName": "Green Earth Foundation"
      }
    ],
    "upcomingEventsCount": 1,
    "pendingRegistrationsCount": 2,
    "attendanceRate": 100.0,
    "averageRating": 4.85,
    "performanceScore": 88.5,
    "badge": "GOLD",
    "certificatesCount": 4,
    "recentActivity": [
      {
        "eventId": 10,
        "eventTitle": "River Cleanup Drive",
        "date": "2026-10-01",
        "hoursWorked": 4.0,
        "status": "APPROVED",
        "rating": 5.0
      }
    ]
  }
  ```

### 13.2 NGO Dashboard
- **Method:** `GET`
- **URL:** `/api/dashboard/ngo` (also accessible via `/dashboard/ngo`)
- **Required Role:** `NGO`
- **Success Status:** `200 OK`
- **Response Body (`NgoDashboardDto`):**
  ```json
  {
    "ngoName": "Green Earth Foundation",
    "verificationStatus": "VERIFIED",
    "eventsCreated": 8,
    "upcomingEventsCount": 2,
    "completedEventsCount": 5,
    "totalApplicants": 45,
    "approvedVolunteersCount": 38,
    "totalVerifiedHours": 152.00,
    "hoursByCategory": [
      {
        "category": "Environment",
        "totalHours": 120.00,
        "eventCount": 6,
        "volunteerCount": 30
      }
    ],
    "activity": [
      {
        "period": "2026-09",
        "label": "Sep 2026",
        "totalHours": 64.00,
        "eventCount": 3,
        "volunteerCount": 18
      }
    ]
  }
  ```

### 13.3 Admin Platform Dashboard
- **Method:** `GET`
- **URL:** `/api/dashboard/admin` (also accessible via `/dashboard/admin`)
- **Required Role:** `ADMIN`
- **Success Status:** `200 OK`
- **Response Body (`AdminDashboardDto`):**
  ```json
  {
    "totalUsers": 25,
    "totalVolunteers": 18,
    "verifiedNgos": 5,
    "pendingNgoVerifications": 1,
    "activeEvents": 8,
    "completedEvents": 5,
    "totalVerifiedHours": 210.50,
    "recentRegistrationsCount": 12,
    "hoursByCategory": [
      {
        "category": "Environment",
        "totalHours": 120.00,
        "eventCount": 6,
        "volunteerCount": 30
      }
    ],
    "activity": [
      {
        "period": "2026-09",
        "label": "Sep 2026",
        "totalHours": 85.00,
        "eventCount": 4,
        "volunteerCount": 24
      }
    ]
  }
  ```

### 13.4 Hours by Category Aggregate Report
- **Method:** `GET`
- **URL:** `/api/reports/hours-by-category` (also accessible via `/reports/hours-by-category`)
- **Query Parameters:** `ngoId` (optional `Long`, restricts to NGO's events)
- **Required Authentication:** Any authenticated user
- **Success Status:** `200 OK`
- **Response Body:** `List<CategoryHoursDto>`:
  ```json
  [
    {
      "category": "Environment",
      "totalHours": 120.50,
      "eventCount": 6,
      "volunteerCount": 32
    },
    {
      "category": "Education",
      "totalHours": 45.00,
      "eventCount": 2,
      "volunteerCount": 14
    }
  ]
  ```

### 13.5 Activity Trends Aggregate Report
- **Method:** `GET`
- **URL:** `/api/reports/activity?granularity={MONTHLY|WEEKLY}&ngoId={Long}` (also `/reports/activity`)
- **Query Parameters:**
  - `granularity`: `MONTHLY` (default) or `WEEKLY`
  - `ngoId`: Optional `Long`
- **Required Authentication:** Any authenticated user
- **Success Status:** `200 OK`
- **Response Body:** `List<ActivityDataPointDto>`:
  ```json
  [
    {
      "period": "2026-08",
      "label": "Aug 2026",
      "totalHours": 50.00,
      "eventCount": 2,
      "volunteerCount": 12
    },
    {
      "period": "2026-09",
      "label": "Sep 2026",
      "totalHours": 85.50,
      "eventCount": 4,
      "volunteerCount": 24
    }
  ]
  ```

### 13.6 Event Reminders Trigger
- **Method:** `POST`
- **URL:** `/api/admin/reminders/trigger?date={yyyy-MM-dd}`
- **Required Role:** `ADMIN`
- **Rule:** Triggers the event reminder notification service manually for a given target date (defaults to tomorrow `LocalDate.now().plusDays(1)`). Idempotent; duplicate notifications are prevented if executed multiple times.
- **Success Status:** `200 OK`
- **Response Body:**
  ```json
  {
    "sentCount": 4,
    "targetDate": "2026-10-14",
    "message": "Sent 4 reminder notifications for events on 2026-10-14"
  }
  ```



