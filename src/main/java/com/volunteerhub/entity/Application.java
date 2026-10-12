package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a volunteer's application/registration for an event.
 */
@Entity
@Table(name = "applications",
       uniqueConstraints = @UniqueConstraint(name = "uk_volunteer_event", columnNames = {"volunteer_id", "event_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private User volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    public Application() {
    }

    public Application(User volunteer, Event event, ApplicationStatus status) {
        this.volunteer = volunteer;
        this.event = event;
        this.status = status != null ? status : ApplicationStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(User volunteer) {
        this.volunteer = volunteer;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
