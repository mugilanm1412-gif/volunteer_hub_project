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

