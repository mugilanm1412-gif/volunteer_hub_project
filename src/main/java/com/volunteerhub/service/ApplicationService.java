package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing volunteer event registrations, approval workflows, pessimistic overbooking locks,
 * and My Events views.
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final NotificationService notificationService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              EventRepository eventRepository,
                              UserRepository userRepository,
                              VolunteerProfileRepository volunteerProfileRepository,
                              NgoProfileRepository ngoProfileRepository,
                              NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.notificationService = notificationService;
    }

    /**
     * Registers a volunteer for an event with pessimistic lock preventing overbooking.
     */
    @Transactional
    public ApplicationDto applyForEvent(Long eventId, String volunteerEmail) {
        User volunteer = findUserByEmail(volunteerEmail);

        // Pessimistic write lock ensures serialized capacity checks
        Event event = eventRepository.findByIdWithLock(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Cannot register for an event that is " + event.getStatus());
        }

        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot register for a past event.");
        }

        // Check for duplicate registration
        if (applicationRepository.existsByVolunteerIdAndEventId(volunteer.getId(), eventId)) {
            throw new ConflictException("You have already registered for this event.");
        }

        // Capacity check via locking read
        List<Application> currentApproved = applicationRepository.findApprovedWithLock(eventId, ApplicationStatus.APPROVED);
        if (currentApproved.size() >= event.getCapacity()) {
            throw new BadRequestException("Event has reached maximum capacity.");
        }

        Application application = new Application(volunteer, event, ApplicationStatus.PENDING);
        application = applicationRepository.save(application);

        String volunteerName = volunteerProfileRepository.findByUserId(volunteer.getId())
            .map(p -> p.getFullName())
            .orElse(volunteer.getEmail());

        // Notify the NGO organizer of the new registration
        notificationService.createNotification(
            event.getNgo(),
            "NEW_REGISTRATION",
            "New Volunteer Registration",
            volunteerName + " applied for '" + event.getTitle() + "'.",
            "APPLICATION",
            application.getId()
        );

        return new ApplicationDto(
            application.getId(),
            event.getId(),
            event.getTitle(),
            volunteer.getId(),
            volunteerName,
            application.getStatus(),
            application.getAppliedAt(),
            "Application submitted successfully. Awaiting NGO review."
        );
    }

    /**
     * Lists all applications for a specific event for NGO organizer review.
     */
    @Transactional(readOnly = true)
    public List<EventApplicationItemDto> getEventApplications(Long eventId, String ngoEmail) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        User ngoUser = findUserByEmail(ngoEmail);

        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to view applications for this event.");
        }

        List<Application> apps = applicationRepository.findByEventIdOrderByAppliedAtAsc(eventId);
        return apps.stream().map(app -> {
            VolunteerProfile profile = volunteerProfileRepository.findByUserId(app.getVolunteer().getId()).orElse(null);
            String name = profile != null ? profile.getFullName() : app.getVolunteer().getEmail();
            String phone = profile != null ? profile.getPhone() : null;
            String skills = profile != null ? profile.getSkills() : null;

            return new EventApplicationItemDto(
                app.getId(),
                app.getVolunteer().getId(),
                name,
                app.getVolunteer().getEmail(),
                phone,
                skills,
                app.getStatus(),
                app.getAppliedAt()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Approves or rejects a volunteer application with pessimistic write lock guarding capacity.
     */
    @Transactional
    public ApplicationDto updateApplicationStatus(Long applicationId, String ngoEmail, ApplicationStatusUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
        User ngoUser = findUserByEmail(ngoEmail);

        if (!application.getEvent().getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to manage this application.");
        }

        ApplicationStatus targetStatus = request.getStatus();
        if (targetStatus != ApplicationStatus.APPROVED && targetStatus != ApplicationStatus.REJECTED) {
            throw new BadRequestException("Status can only be updated to APPROVED or REJECTED.");
        }

        if (targetStatus == ApplicationStatus.APPROVED) {
            // Obtain lock on event to serialize capacity evaluations
            Event event = eventRepository.findByIdWithLock(application.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

            List<Application> currentApproved = applicationRepository.findApprovedWithLock(event.getId(), ApplicationStatus.APPROVED);
            if (currentApproved.size() >= event.getCapacity()) {
                throw new BadRequestException("Cannot approve registration: Event has reached maximum capacity (" +
                    event.getCapacity() + ").");
            }

            application.setStatus(ApplicationStatus.APPROVED);
            application = applicationRepository.saveAndFlush(application);

            notificationService.createNotification(
                application.getVolunteer(),
                "APPLICATION_APPROVED",
                "Application Approved",
                "Your registration for '" + event.getTitle() + "' was approved by the organizer!",
                "EVENT",
                event.getId()
            );
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            application = applicationRepository.save(application);

            notificationService.createNotification(
                application.getVolunteer(),
                "APPLICATION_REJECTED",
                "Application Update",
                "Your registration for '" + application.getEvent().getTitle() + "' was not accepted at this time.",
                "EVENT",
                application.getEvent().getId()
            );
        }

        return new ApplicationDto(
            application.getId(),
            application.getEvent().getId(),
            application.getEvent().getTitle(),
            application.getVolunteer().getId(),
            null,
            application.getStatus(),
            application.getAppliedAt(),
            "Application status updated to " + application.getStatus()
        );
    }

    /**
     * Cancels a volunteer application.
     */
    @Transactional
    public ApplicationDto cancelApplication(Long applicationId, String volunteerEmail) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));
        User volunteer = findUserByEmail(volunteerEmail);

        if (!application.getVolunteer().getId().equals(volunteer.getId())) {
            throw new ForbiddenException("You do not have permission to cancel this registration.");
        }

        if (application.getEvent().getEventDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot cancel registration for a past event.");
        }

        if (application.getStatus() == ApplicationStatus.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled.");
        }

        application.setStatus(ApplicationStatus.CANCELLED);
        application = applicationRepository.save(application);

        return new ApplicationDto(
            application.getId(),
            application.getEvent().getId(),
            application.getEvent().getTitle(),
            volunteer.getId(),
            null,
            application.getStatus(),
            application.getAppliedAt(),
            "Registration cancelled successfully."
        );
    }

    /**
     * Retrieves paginated applications for the authenticated volunteer ("My Events").
     */
    @Transactional(readOnly = true)
    public PageResponseDto<VolunteerApplicationDto> getVolunteerApplications(String volunteerEmail,
                                                                             String tab,
                                                                             Pageable pageable) {
        User volunteer = findUserByEmail(volunteerEmail);
        LocalDate today = LocalDate.now();

        Page<Application> page;
        String normalizedTab = tab != null ? tab.trim().toLowerCase() : "all";

        switch (normalizedTab) {
            case "upcoming":
                page = applicationRepository.findUpcomingApplications(volunteer.getId(), today, pageable);
                break;
            case "completed":
                page = applicationRepository.findCompletedApplications(volunteer.getId(), today, pageable);
                break;
            case "pending":
                page = applicationRepository.findPendingApplications(volunteer.getId(), pageable);
                break;
            case "cancelled":
                page = applicationRepository.findCancelledOrRejectedApplications(volunteer.getId(), pageable);
                break;
            case "all":
            default:
                page = applicationRepository.findAllVolunteerApplications(volunteer.getId(), pageable);
                break;
        }

        List<VolunteerApplicationDto> dtos = page.getContent().stream().map(app -> {
            Event e = app.getEvent();
            NgoProfile ngoProfile = ngoProfileRepository.findByUserId(e.getNgo().getId()).orElse(null);
            String ngoName = ngoProfile != null ? ngoProfile.getName() : e.getNgo().getEmail();

            return new VolunteerApplicationDto(
                app.getId(),
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                ngoName,
                e.getCategory(),
                e.getLocation(),
                e.getEventDate(),
                e.getStartTime(),
                e.getEndTime(),
                app.getStatus(),
                e.getStatus(),
                app.getAppliedAt()
            );
        }).collect(Collectors.toList());

        return new PageResponseDto<>(
            dtos,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
