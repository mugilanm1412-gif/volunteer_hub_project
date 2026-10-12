# Volunteer Hub: Entity-Relationship Diagram

This diagram visualizes the relational schema implemented in Flyway migrations (`V1__create_schema.sql`).

```mermaid
erDiagram
    USERS ||--o| VOLUNTEER_PROFILES : "has profile"
    USERS ||--o| NGO_PROFILES : "has profile"
    USERS ||--o{ EVENTS : "organizes (as NGO)"
    USERS ||--o{ APPLICATIONS : "applies (as Volunteer)"
    EVENTS ||--o{ APPLICATIONS : "receives"
    APPLICATIONS ||--o| ATTENDANCE : "results in"
    USERS ||--o{ ATTENDANCE : "marks / reviews"
    ATTENDANCE ||--o| VOLUNTEER_RATINGS : "evaluated by NGO"
    ATTENDANCE ||--o| CERTIFICATES : "generates upon approval"
    USERS ||--o{ NOTIFICATIONS : "receives"
    USERS ||--o{ PASSWORD_RESET_TOKENS : "requests"
    USERS ||--o{ AUDIT_LOGS : "triggers action"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar role "VOLUNTEER, NGO, ADMIN"
        varchar status "ACTIVE, SUSPENDED, DEACTIVATED"
        timestamp created_at
    }

    VOLUNTEER_PROFILES {
        bigint id PK
        bigint user_id FK, UK
        varchar full_name
        varchar phone
        varchar city
        varchar skills
        varchar interests
        text bio
        boolean show_on_leaderboard
        timestamp created_at
    }

    NGO_PROFILES {
        bigint id PK
        bigint user_id FK, UK
        varchar name
        varchar cause
        text description
        varchar registration_number
        varchar contact_person
        varchar contact_phone
        varchar address
        varchar verification_status "PENDING, VERIFIED, REJECTED"
        text rejection_reason
        timestamp created_at
    }

    EVENTS {
        bigint id PK
        bigint ngo_id FK
        varchar title
        text description
        varchar category
        varchar location
        date event_date
        time start_time
        time end_time
        varchar required_skills
        int capacity
        varchar status "DRAFT, PUBLISHED, CANCELLED, COMPLETED, REMOVED"
        text removed_reason
        bigint version "Optimistic Locking"
        timestamp created_at
    }

    APPLICATIONS {
        bigint id PK
        bigint volunteer_id FK
        bigint event_id FK
        varchar status "PENDING, APPROVED, REJECTED, CANCELLED"
        timestamp applied_at
    }

    ATTENDANCE {
        bigint id PK
        bigint application_id FK, UK
        varchar attendance_status "PRESENT, ABSENT"
        decimal hours_worked
        varchar hours_status "PENDING, APPROVED, REJECTED"
        bigint marked_by FK
        bigint reviewed_by FK
        timestamp reviewed_at
        text reject_reason
        timestamp created_at
    }

    VOLUNTEER_RATINGS {
        bigint id PK
        bigint attendance_id FK, UK
        int punctuality "1-5"
        int teamwork "1-5"
        int effort "1-5"
        text comment
        bigint rated_by FK
        timestamp created_at
    }

    CERTIFICATES {
        bigint id PK
        bigint attendance_id FK, UK
        varchar certificate_number UK
        timestamp issued_on
    }

    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar type
        varchar title
        text message
        boolean is_read
        varchar related_entity_type
        bigint related_entity_id
        timestamp created_at
    }

    PASSWORD_RESET_TOKENS {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        timestamp expires_at
        timestamp used_at
        timestamp created_at
    }

    AUDIT_LOGS {
        bigint id PK
        bigint actor_id FK
        varchar action
        varchar entity_type
        bigint entity_id
        text details
        timestamp created_at
    }
```

## Architectural Highlights
1. **Zero Redundant Columns**: Total hours, attendance rate %, average rating, performance score, and badge tiers are aggregated from trusted `attendance` and `volunteer_ratings` records at query time.
2. **Strict Foreign Keys**: Cascading deletions protect user isolation, while attendance and certificates maintain strict 1-to-1 relationships with their parent records.
3. **Optimistic Locking**: `events.version` safeguards concurrent event updates, complementing pessimistic locking (`PESSIMISTIC_WRITE`) used during slot reservations in Phase 3.
