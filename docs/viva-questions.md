# Volunteer Hub: Technical Viva Voce Questions & Answers

This document provides a comprehensive, structured question-and-answer guide designed for final-year college evaluation, project defense, and technical interviews. All explanations are written in clear, simple English with direct references to the **Volunteer Hub and Performance Tracking System** codebase.

---

## Table of Contents
1. [Core Java & Java 21 LTS](#1-core-java--java-21-lts)
2. [Spring Boot Framework Architecture](#2-spring-boot-framework-architecture)
3. [Spring Security, Authentication & CSRF](#3-spring-security-authentication--csrf)
4. [JPA, Hibernate & JDBC Persistence](#4-jpa-hibernate--jdbc-persistence)
5. [Database Design & MySQL 8](#5-database-design--mysql-8)
6. [RESTful API Design & HTTP Protocols](#6-restful-api-design--http-protocols)
7. [Volunteer Hub Proprietary Business Logic](#7-volunteer-hub-proprietary-business-logic)

---

## 1. Core Java & Java 21 LTS

### Q1.1: Why did you choose Java 21 LTS for this project?
**Answer:** Java 21 is a Long-Term Support (LTS) release that provides modern performance enhancements, robust stability, and language ergonomics:
- **Record classes and pattern matching:** Simplifies immutability and Data Transfer Objects (DTOs).
- **Strong encapsulation and standard APIs:** Enhanced `java.time` package (used extensively for event start/end times and scheduled duration calculations).
- **Cryptographic primitives:** `java.security.MessageDigest` and `HexFormat` used for hashing password reset tokens and certificate verification digests.

### Q1.2: What is the difference between an Abstract Class and an Interface in Java?
**Answer:**
- **Interface:** Defines a contract of *what* a class should do without specifying *how*. A class can implement multiple interfaces (multiple inheritance of type). In our project, Spring Data repositories (e.g., `EventRepository`, `ApplicationRepository`) extend `JpaRepository`, which is an interface.
- **Abstract Class:** A partially implemented class that serves as a common base. A class can extend only one abstract class.

### Q1.3: How does Java handle multithreading and concurrency?
**Answer:** Java manages multithreading via the `java.lang.Thread` class and the high-level `java.util.concurrent` framework (`ExecutorService`, `Future`, `CountDownLatch`). In Phase 8a testing, we used `Executors.newFixedThreadPool(5)` and `CountDownLatch` to simulate 5 concurrent threads attacking an event with capacity = 1 to prove that our database lock prevents race conditions.

---

## 2. Spring Boot Framework Architecture

### Q2.1: What is Spring Boot and how does it differ from traditional Spring?
**Answer:**
- **Traditional Spring:** Required extensive manual XML configuration, external servlet container deployment (like Apache Tomcat), and complex dependency management.
- **Spring Boot:** Provides **convention over configuration**, an **embedded web server** (embedded Tomcat on port 8080), starter dependencies (like `spring-boot-starter-web`), and automated configuration (`@SpringBootApplication`).

### Q2.2: What is Inversion of Control (IoC) and Dependency Injection (DI)?
**Answer:**
- **Inversion of Control (IoC):** Instead of the developer manually creating objects using `new ServiceClass()`, control of object lifecycle and creation is inverted and handed over to the Spring IoC Container.
- **Dependency Injection (DI):** The mechanism by which the Spring Container provides (injects) dependent beans into an object. We use **constructor-based dependency injection** across all services and controllers (e.g., `ApplicationService` receiving `EventRepository` via constructor), which ensures immutability and easy testability without reflection.

### Q2.3: What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?
**Answer:** All four are Spring stereotype annotations that register classes as managed Spring Beans in the Application Context:
- `@Component`: General-purpose Spring bean.
- `@Service`: Marks business logic layer classes (e.g., `PerformanceCalculator`, `CertificateService`).
- `@Repository`: Marks the data access layer, automatically translating database SQLExceptions into Spring's unified `DataAccessException` hierarchy.
- `@RestController`: Specialized for web REST APIs, combining `@Controller` and `@ResponseBody` so that method return values are automatically serialized into JSON.

---

## 3. Spring Security, Authentication & CSRF

### Q3.1: How does authentication work in Volunteer Hub?
**Answer:** The project uses **Session-based Authentication** backed by Spring Security:
1. The client submits credentials (`email` and `password`) to `POST /api/auth/login`.
2. Spring's `DaoAuthenticationProvider` retrieves user details from `CustomUserDetailsService` and verifies the password using `BCryptPasswordEncoder`.
3. Upon success, Spring creates an HTTP session and sends back an HTTP-only cookie named `JSESSIONID`.
4. Subsequent requests include this cookie, which Spring's `SecurityContextHolderFilter` resolves to the authenticated `CustomUserDetails` principal.

### Q3.2: What is Cross-Site Request Forgery (CSRF) and how does our application prevent it?
**Answer:**
- **The Threat:** In CSRF, a malicious website tricks a user's browser into executing an unauthorized state-changing request on our site because the browser automatically attaches cookies (like `JSESSIONID`).
- **Our Defense:** We configure `CookieCsrfTokenRepository.withHttpOnlyFalse()`:
  - Spring sends a random anti-forgery token in a readable cookie called `XSRF-TOKEN`.
  - For every state-mutating request (`POST`, `PUT`, `PATCH`, `DELETE`), the client JavaScript (`api.js`) must read this cookie and inject its value into the HTTP request header `X-XSRF-TOKEN`.
  - An external malicious site cannot read our domain's cookies due to the browser's **Same-Origin Policy (SOP)**, preventing attackers from injecting the valid header.

### Q3.3: Why do we use BCrypt for passwords instead of MD5 or SHA-256?
**Answer:** MD5 and SHA-256 are fast hash functions designed for message integrity; they can be cracked at billions of guesses per second using GPUs and rainbow tables. **BCrypt** is an adaptive, salted, slow hashing algorithm that includes an exponential work factor (computational cost), making brute-force and rainbow table attacks virtually impossible.

### Q3.4: How are Role-Based Access Controls (RBAC) enforced?
**Answer:** We enforce security at two complementary levels:
1. **URL Filter Level (`SecurityConfig.java`):**
   - `/api/volunteers/**` requires role `ROLE_VOLUNTEER`.
   - `/api/ngos/**` requires role `ROLE_NGO`.
   - `/api/admin/**` requires role `ROLE_ADMIN`.
2. **Method Level (`@EnableMethodSecurity`):**
   - Controller methods annotate fine-grained authorization rules using `@PreAuthorize("hasRole('VOLUNTEER')")` or `@PreAuthorize("hasRole('NGO')")`.

---

## 4. JPA, Hibernate & JDBC Persistence

### Q4.1: What is the relationship between JDBC, JPA, and Hibernate?
**Answer:**
- **JDBC (Java Database Connectivity):** The low-level standard API for connecting Java applications to relational databases (writing raw SQL strings and iterating over `ResultSet`).
- **JPA (Jakarta Persistence API):** A standard Java specification that defines how Object-Relational Mapping (ORM) should work using annotations like `@Entity`, `@Table`, `@Id`. JPA is an interface, not an implementation.
- **Hibernate:** The actual ORM implementation provider that implements the JPA specification, generating optimized SQL queries and managing object caching and dirty checking.

### Q4.2: What is "Dirty Checking" in Hibernate?
**Answer:** Inside a method annotated with `@Transactional`, Hibernate automatically tracks changes made to managed entities. When the transaction commits, Hibernate compares the entity's current state with its initial snapshot and automatically issues an `UPDATE` SQL statement without requiring an explicit call to `repository.save()`.

### Q4.3: What is the N+1 query problem and how do we solve it?
**Answer:**
- **Problem:** When fetching a list of $N$ entities (e.g., 20 `Application` entities) that have a `LAZY` relation to another entity (`Event`), Hibernate executes 1 query to get the applications, and then $N$ separate queries to fetch the event for each application ($1 + N = 21$ queries).
- **Our Solution:** We use **`JOIN FETCH`** in our repository `@Query` annotations (e.g., `SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n`). This instructs Hibernate to load the parent and related child entities in a single SQL `JOIN` query.

### Q4.4: Why do we use Flyway database migration instead of `spring.jpa.hibernate.ddl-auto=update`?
**Answer:**
- `ddl-auto=update` is dangerous for production: it can alter tables unpredictably, fails to rename columns cleanly, and cannot track historical schema modifications.
- **Flyway** provides version-controlled, repeatable SQL migration scripts (`V1__...`, `V2__...`). It maintains a metadata table `flyway_schema_history` and guarantees that every developer environment and production instance runs the exact same schema.

---

## 5. Database Design & MySQL 8

### Q5.1: What is the difference between Optimistic Locking and Pessimistic Locking?
**Answer:**
- **Optimistic Locking:** Assumes conflicts are rare. Uses a `@Version` integer column. If two users try to update simultaneously, the second transaction detects a version mismatch and throws an `OptimisticLockException`.
- **Pessimistic Locking:** Assumes conflicts are likely. The database physically locks the affected row at query time (`SELECT ... FOR UPDATE`). Other transactions attempting to acquire the lock are forced to wait until the current transaction commits or rolls back.

### Q5.2: How does Volunteer Hub prevent event overbooking under high concurrent load?
**Answer:** We implement **Pessimistic Write Locking** in `EventRepository`:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT e FROM Event e WHERE e.id = :id")
Optional<Event> findByIdWithLock(@Param("id") Long id);
```
When an NGO approves an application, the transaction calls `findByIdWithLock(eventId)`. MySQL executes `SELECT ... FOR UPDATE`, placing an exclusive row-level lock on the event. Inside this atomic transaction, the service counts currently approved applications against `event.getCapacity()`. Any competing approval thread waits for the lock, reads the freshly updated count, and is safely rejected once capacity is reached.

### Q5.3: What are the ACID properties in database management?
**Answer:**
- **Atomicity:** All operations within a transaction succeed together or fail together (e.g., approving hours AND issuing a certificate occur as one atomic unit).
- **Consistency:** The database transitions from one valid state to another, preserving all foreign keys and check constraints.
- **Isolation:** Concurrent transactions execute independently without cross-transaction interference (controlled by isolation levels like `REPEATABLE READ`).
- **Durability:** Once committed, transaction updates are permanently written to disk logs (MySQL write-ahead redo log) and survive crashes.

---

## 6. RESTful API Design & HTTP Protocols

### Q6.1: What are the core HTTP methods and which ones are idempotent?
**Answer:**
- `GET`: Safe and idempotent; retrieves resources without modifying state.
- `POST`: Non-idempotent; creates new resources or executes non-idempotent commands (e.g., submitting applications).
- `PUT`: Idempotent; completely replaces an existing resource with the provided payload.
- `PATCH`: Partially modifies specific fields of an existing resource (e.g., updating application status or verification).
- `DELETE`: Idempotent; removes the resource.

### Q6.2: What is the difference between HTTP 401 Unauthorized and HTTP 403 Forbidden?
**Answer:**
- **HTTP 401 Unauthorized:** The user has not provided authentication credentials, or their session cookie has expired (*"Who are you?"*).
- **HTTP 403 Forbidden:** The user is authenticated, but their assigned role or identity lacks permission to access the resource (*"I know who you are, but you cannot enter"*). For example, a volunteer attempting to access `/api/admin/users`.

### Q6.3: Why do controllers return DTOs rather than direct JPA entities?
**Answer:** Returning JPA entities directly causes severe architectural issues:
1. **Security leak:** May expose internal fields like `passwordHash` or deleted flags.
2. **Infinite recursion:** Bidirectional relationships (`Event -> Application -> Event`) cause Jackson JSON serialization stack overflow crashes.
3. **API Coupling:** Internal database refactoring directly breaks external clients. DTOs act as a clean, stable contract.

---

## 7. Volunteer Hub Proprietary Business Logic

### Q7.1: How is the volunteer performance score calculated?
**Answer:** The performance score is computed out of 100 using a balanced mathematical formula:
$$\text{Performance Score} = 0.40 \cdot H_{\text{score}} + 0.30 \cdot A_{\text{rate}} + 0.30 \cdot R_{\text{score}}$$
Where:
- **$H_{\text{score}}$ (Hours Component):** $\min(100, \text{Verified Hours} \times 2.0)$. Cap of 50 hours achieves 100 points.
- **$A_{\text{rate}}$ (Attendance Rate):** $\frac{\text{Attended Events}}{\text{Approved Applications}} \times 100$.
- **$R_{\text{score}}$ (Rating Component):** $\frac{\text{Average Rating}}{5.0} \times 100$.
- **Smart Redistribution:** If a new volunteer has attended events but the NGO has not yet submitted a rating, the unrated 30% rating weight is dynamically redistributed proportionally across hours (57.14%) and attendance (42.86%), ensuring newcomers are never penalized with a zero score.

### Q7.2: What are the badge tiers and boundary values?
**Answer:**
- **NEWCOMER:** Score $0.0$ to $39.99$
- **BRONZE:** Score $40.0$ to $64.99$
- **SILVER:** Score $65.0$ to $84.99$
- **GOLD:** Score $85.0$ to $100.0$
Our unit tests in `PerformanceCalculatorTests.java` explicitly verify exact boundary values (e.g., 39.9 vs 40.0, 64.9 vs 65.0, 84.9 vs 85.0).

### Q7.3: How is certificate generation guaranteed to be idempotent?
**Answer:** When an NGO approves a volunteer's service hours (`PATCH /api/attendance/{id}/approval`), `CertificateService.issueCertificateForAttendance(attendance)` is called:
1. The service checks `certificateRepository.findByAttendanceId(attendance.getId())`.
2. If a certificate already exists, the existing record is returned without re-generation.
3. If absent, a unique certificate number is minted (`CERT-YYYY-XXXXXX`) using sequential zero-padded indexing, and an OpenPDF document is generated.
This guarantees that repeated clicks or duplicate approval requests never spawn duplicate certificates.

### Q7.4: How does the event reminder scheduler prevent duplicate reminder notifications?
**Answer:** `EventReminderScheduler` runs daily at 8:00 AM (or via manual admin trigger). It finds published events occurring on the next calendar day (`LocalDate.now().plusDays(1)`) and approved attendees.
To guarantee **zero duplicate notifications**, it logs each reminder in the `audit_logs` table with action `REMINDER_SENT` and details format `EventReminder:EventId={id}:VolunteerId={id}:Date={date}`. Before creating a notification, it checks if this audit key exists; if found, it idempotently skips the notification.
