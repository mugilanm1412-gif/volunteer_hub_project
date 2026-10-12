package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.ApplicationRepository;
import com.volunteerhub.repository.EventRepository;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing event creation, modification, lifecycle status, and retrieval.
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        NgoProfileRepository ngoProfileRepository,
                        ApplicationRepository applicationRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new event. Only VERIFIED NGOs are permitted to create events.
     */
    @Transactional
    public EventDto createEvent(String ngoEmail, EventCreateRequest request) {
        User ngoUser = findUserByEmail(ngoEmail);
        NgoProfile ngoProfile = ngoProfileRepository.findByUserId(ngoUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user: " + ngoEmail));

        // Enforce verified-NGO rule
        if (ngoProfile.getVerificationStatus() != NgoVerificationStatus.VERIFIED) {
            throw new ForbiddenException("Only verified NGOs can create events. Your current status is: " +
                ngoProfile.getVerificationStatus());
        }

        // Validate time ordering
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Event end time must be after start time.");
        }

        Event event = new Event(
            ngoUser,
            request.getTitle().trim(),
            request.getDescription().trim(),
            request.getCategory().trim(),
            request.getLocation().trim(),
            request.getEventDate(),
            request.getStartTime(),
            request.getEndTime(),
            request.getRequiredSkills() != null ? request.getRequiredSkills().trim() : null,
            request.getCapacity(),
            request.getStatus() != null ? request.getStatus() : EventStatus.PUBLISHED
        );

        event = eventRepository.save(event);
        return toDto(event, null);
    }

    /**
     * Updates an existing event. Enforces ownership and validation rules.
     */
    @Transactional
    public EventDto updateEvent(Long eventId, String ngoEmail, EventUpdateRequest request) {
        Event event = findEventById(eventId);
        User ngoUser = findUserByEmail(ngoEmail);

        // Enforce ownership
        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to edit this event.");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot edit a cancelled event.");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Event end time must be after start time.");
        }

        long approvedCount = applicationRepository.countByEventIdAndStatus(eventId, ApplicationStatus.APPROVED);
        if (request.getCapacity() < approvedCount) {
            throw new BadRequestException("Capacity cannot be reduced below the number of approved registrations (" +
                approvedCount + ").");
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription().trim());
        event.setCategory(request.getCategory().trim());
        event.setLocation(request.getLocation().trim());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setRequiredSkills(request.getRequiredSkills() != null ? request.getRequiredSkills().trim() : null);
        event.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        event = eventRepository.save(event);
        return toDto(event, null);
    }

    /**
     * Deletes an event. Permitted only if the event has zero registrations.
     */
    @Transactional
    public void deleteEvent(Long eventId, String ngoEmail) {
        Event event = findEventById(eventId);
        User ngoUser = findUserByEmail(ngoEmail);

        // Enforce ownership
        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to delete this event.");
        }

        long registrationCount = applicationRepository.countByEventId(eventId);
        if (registrationCount > 0) {
            throw new BadRequestException("Cannot delete event with existing registrations (" +
                registrationCount + "). Please cancel the event instead.");
        }

        eventRepository.delete(event);
    }

    /**
     * Cancels an event and notifies all registered volunteers.
     */
    @Transactional
    public EventDto cancelEvent(Long eventId, String ngoEmail, EventCancelRequest request) {
        Event event = findEventById(eventId);
        User ngoUser = findUserByEmail(ngoEmail);

        // Enforce ownership
        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to cancel this event.");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event is already cancelled.");
        }

        String reason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
            ? request.getReason().trim()
            : "Event has been cancelled by the organizer.";

        event.setStatus(EventStatus.CANCELLED);
        event.setRemovedReason(reason);
        event = eventRepository.save(event);

        // Notify all registered volunteers
        List<Application> applications = applicationRepository.findByEventIdOrderByAppliedAtAsc(eventId);
        for (Application app : applications) {
            notificationService.createNotification(
                app.getVolunteer(),
                "EVENT_CANCELLED",
                "Event Cancelled: " + event.getTitle(),
                "The event '" + event.getTitle() + "' has been cancelled by the organizer. Reason: " + reason,
                "EVENT",
                event.getId()
            );
        }

        return toDto(event, null);
    }

    /**
     * Marks an event as COMPLETED.
     */
    @Transactional
    public EventDto completeEvent(Long eventId, String ngoEmail) {
        Event event = findEventById(eventId);
        User ngoUser = findUserByEmail(ngoEmail);

        // Enforce ownership
        if (!event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to complete this event.");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a cancelled event.");
        }

        event.setStatus(EventStatus.COMPLETED);
        event = eventRepository.save(event);
        return toDto(event, null);
    }

    /**
     * Retrieves event details by ID, enriches with user application status if logged in.
     */
    @Transactional(readOnly = true)
    public EventDto getEventById(Long eventId, String currentUserEmail) {
        Event event = findEventById(eventId);

        String userApplicationStatus = null;
        if (currentUserEmail != null) {
            userRepository.findByEmail(currentUserEmail).ifPresent(user -> {
                applicationRepository.findByVolunteerIdAndEventId(user.getId(), eventId)
                    .ifPresent(app -> {
                        // User application status
                    });
            });
            User user = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (user != null) {
                userApplicationStatus = applicationRepository.findByVolunteerIdAndEventId(user.getId(), eventId)
                    .map(a -> a.getStatus().name())
                    .orElse(null);
            }
        }

        return toDto(event, userApplicationStatus);
    }

    /**
     * Lists events created by the authenticated NGO.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<EventDto> getNgoEvents(String ngoEmail, Pageable pageable) {
        User ngoUser = findUserByEmail(ngoEmail);
        Page<Event> page = eventRepository.findByNgoId(ngoUser.getId(), pageable);
        List<EventDto> dtoList = page.getContent().stream()
            .map(e -> toDto(e, null))
            .collect(Collectors.toList());

        return new PageResponseDto<>(
            dtoList,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    /**
     * Searches published events with optional filters.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<EventDto> searchEvents(String q, String category, String location,
                                                  LocalDate from, LocalDate to, Boolean hasSlots,
                                                  Pageable pageable) {
        Page<Event> page = eventRepository.searchEvents(
            EventStatus.PUBLISHED,
            (category != null && !category.trim().isEmpty()) ? category.trim() : null,
            (location != null && !location.trim().isEmpty()) ? location.trim() : null,
            from,
            to,
            (q != null && !q.trim().isEmpty()) ? q.trim() : null,
            hasSlots,
            pageable
        );

        List<EventDto> dtos = page.getContent().stream()
            .map(e -> toDto(e, null))
            .collect(Collectors.toList());

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

    private Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
    }

    public EventDto toDto(Event event, String userApplicationStatus) {
        NgoProfile ngoProfile = ngoProfileRepository.findByUserId(event.getNgo().getId()).orElse(null);
        String ngoName = ngoProfile != null ? ngoProfile.getName() : event.getNgo().getEmail();
        String ngoContactPerson = ngoProfile != null ? ngoProfile.getContactPerson() : null;
        String ngoContactPhone = ngoProfile != null ? ngoProfile.getContactPhone() : null;

        int approvedCount = (int) applicationRepository.countByEventIdAndStatus(event.getId(), ApplicationStatus.APPROVED);
        int remainingSlots = Math.max(0, event.getCapacity() - approvedCount);

        return new EventDto(
            event.getId(),
            event.getNgo().getId(),
            ngoName,
            ngoContactPerson,
            ngoContactPhone,
            event.getTitle(),
            event.getDescription(),
            event.getCategory(),
            event.getLocation(),
            event.getEventDate(),
            event.getStartTime(),
            event.getEndTime(),
            event.getRequiredSkills(),
            event.getCapacity(),
            approvedCount,
            remainingSlots,
            event.getStatus(),
            event.getRemovedReason(),
            userApplicationStatus,
            event.getCreatedAt()
        );
    }
}
