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
