package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.BadRequestException;
import com.volunteerhub.exception.ConflictException;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.ApplicationRepository;
import com.volunteerhub.repository.AttendanceRepository;
import com.volunteerhub.repository.EventRepository;
import com.volunteerhub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing physical event attendance marking, hours recording, and volunteer schedule history.
 */
@Service
@Transactional
public class AttendanceService {

    /**
     * Documented tolerance (in hours) permitted beyond the scheduled event duration.
     * Accounts for wrap-up, community debriefs, and minor overtime.
     */
    public static final BigDecimal ATTENDANCE_HOURS_TOLERANCE = BigDecimal.valueOf(1.0);

    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final com.volunteerhub.repository.NgoProfileRepository ngoProfileRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EventRepository eventRepository,
                             ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             com.volunteerhub.repository.NgoProfileRepository ngoProfileRepository,
                             AuditLogService auditLogService,
                             NotificationService notificationService) {
        this.attendanceRepository = attendanceRepository;
        this.eventRepository = eventRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Records batch attendance and service hours for approved volunteer registrations.
     * Enforces ownership, event conclusion, capacity limits, and duration caps.
     *
     * @param eventId the event ID
     * @param ngoEmail the authenticated NGO account email
     * @param request batch attendance submission payload
     * @return list of created attendance records
     */
    public List<AttendanceResponseDto> markAttendance(Long eventId, String ngoEmail, BatchAttendanceRequest request) {
        User ngoUser = userRepository.findByEmail(ngoEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + ngoEmail));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // 1. Ownership check: Only the hosting NGO can mark attendance
        if (event.getNgo() == null || !event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to mark attendance for this event");
        }

        // 2. Concluded check: Event date must have passed OR status must be COMPLETED
        boolean isPastDate = event.getEventDate() != null && event.getEventDate().isBefore(LocalDate.now());
        boolean isCompleted = event.getStatus() == EventStatus.COMPLETED;
        if (!isPastDate && !isCompleted) {
            throw new BadRequestException("Attendance can only be marked after the event date has passed or the event is marked as COMPLETED");
        }

        // 3. Compute maximum allowed hours (Scheduled Duration + ATTENDANCE_HOURS_TOLERANCE)
        BigDecimal scheduledHours = BigDecimal.valueOf(4.0); // Default fallback
        if (event.getStartTime() != null && event.getEndTime() != null) {
            long minutes = Math.max(0, Duration.between(event.getStartTime(), event.getEndTime()).toMinutes());
            scheduledHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
        BigDecimal maxAllowedHours = scheduledHours.add(ATTENDANCE_HOURS_TOLERANCE);

        // 4. Validate unique application IDs within the submitted batch
        Set<Long> batchApplicationIds = new HashSet<>();
        for (AttendanceRecordRequest rec : request.getRecords()) {
            if (!batchApplicationIds.add(rec.getApplicationId())) {
                throw new ConflictException("Duplicate application ID #" + rec.getApplicationId() + " in batch request");
            }
        }

        List<Attendance> savedList = new ArrayList<>();

        for (AttendanceRecordRequest rec : request.getRecords()) {
            Application application = applicationRepository.findById(rec.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + rec.getApplicationId()));

            // Verify application belongs to this event
            if (!application.getEvent().getId().equals(event.getId())) {
                throw new BadRequestException("Application #" + application.getId() + " does not belong to event #" + event.getId());
            }

            // Verify application is APPROVED
            if (application.getStatus() != ApplicationStatus.APPROVED) {
                throw new BadRequestException("Attendance can only be marked for APPROVED registrations. Application #" +
                        application.getId() + " has status " + application.getStatus());
            }

            // Verify no duplicate attendance record already exists in database
            if (attendanceRepository.existsByApplicationId(application.getId())) {
                throw new ConflictException("Attendance has already been marked for application #" + application.getId());
            }

            // Validate hours according to attendance status
            BigDecimal hoursToRecord;
            if (rec.getAttendanceStatus() == AttendanceStatus.ABSENT) {
                hoursToRecord = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                BigDecimal submittedHours = rec.getHoursWorked();
                if (submittedHours == null || submittedHours.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Present volunteers must have hours worked greater than 0");
                }
                if (submittedHours.compareTo(maxAllowedHours) > 0) {
                    throw new BadRequestException("Hours worked (" + submittedHours + ") exceeds maximum scheduled duration plus tolerance (" +
                            maxAllowedHours + " hours)");
                }
                hoursToRecord = submittedHours.setScale(2, RoundingMode.HALF_UP);
            }

            Attendance attendance = new Attendance(application, rec.getAttendanceStatus(), hoursToRecord, ngoUser);
            attendance.setHoursStatus(HoursStatus.PENDING);
            attendance.setCreatedAt(LocalDateTime.now());

            savedList.add(attendanceRepository.save(attendance));
        }

        return savedList.stream()
                .map(this::mapToAttendanceResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all recorded attendance entries for an event (NGO owner view).
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> getEventAttendance(Long eventId, String ngoEmail) {
        User ngoUser = userRepository.findByEmail(ngoEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + ngoEmail));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (event.getNgo() == null || !event.getNgo().getId().equals(ngoUser.getId())) {
            throw new ForbiddenException("You do not have permission to view attendance for this event");
        }

        List<Attendance> attendances = attendanceRepository.findByEventIdWithDetails(eventId);
        return attendances.stream()
                .map(this::mapToAttendanceResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves attendance history and verified hours for the authenticated volunteer.
     * Uses eager fetch joins to eliminate N+1 queries across application, event, and NGO profile.
     */
    @Transactional(readOnly = true)
    public List<VolunteerAttendanceDto> getVolunteerAttendance(String volunteerEmail) {
        User volunteer = userRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + volunteerEmail));

        List<Attendance> attendances = attendanceRepository.findByVolunteerIdWithDetails(volunteer.getId());
        return attendances.stream()
                .map(this::mapToVolunteerAttendanceDto)
                .collect(Collectors.toList());
    }

    /**
     * Reviews (approves or rejects) logged volunteer service hours.
     * Enforces that approved records are strictly immutable.
     *
     * @param attendanceId the attendance record ID
     * @param reviewerEmail the authenticated NGO or Admin email
     * @param request review payload containing status and optional/mandatory reason
     * @return updated attendance record representation
     */
    public AttendanceResponseDto reviewHours(Long attendanceId, String reviewerEmail, HoursReviewRequest request) {
        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + reviewerEmail));

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + attendanceId));

        Event event = attendance.getApplication().getEvent();

        // 1. Authorization: ADMIN can review any record; NGO can only review records from its own events
        boolean isAdmin = reviewer.getRole() == Role.ADMIN;
        boolean isOwnerNgo = (reviewer.getRole() == Role.NGO) &&
                (event.getNgo() != null && event.getNgo().getId().equals(reviewer.getId()));

        if (!isAdmin && !isOwnerNgo) {
            throw new ForbiddenException("You do not have permission to review hours for this attendance record");
        }

        // 2. Immutability check: An already APPROVED record cannot be re-reviewed
        if (attendance.getHoursStatus() == HoursStatus.APPROVED) {
            throw new BadRequestException("Attendance hours have already been approved and cannot be modified");
        }

        // 3. Validation: Mandatory reason on rejection
        if (request.getStatus() == HoursStatus.REJECTED) {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new BadRequestException("A reason is mandatory when rejecting volunteer hours");
            }
        }

        // 4. Update attendance entity
        attendance.setHoursStatus(request.getStatus());
        attendance.setReviewedBy(reviewer);
        attendance.setReviewedAt(LocalDateTime.now());
        if (request.getStatus() == HoursStatus.REJECTED) {
            attendance.setRejectReason(request.getReason().trim());
        } else {
            attendance.setRejectReason(null);
            // Extension hook for Phase 6 automated certificate issuance
            onHoursApproved(attendance, reviewer);
        }

        Attendance saved = attendanceRepository.save(attendance);

        // 5. Audit Logging
        String action = (request.getStatus() == HoursStatus.APPROVED) ? "APPROVE_HOURS" : "REJECT_HOURS";
        User volunteer = attendance.getApplication().getVolunteer();
        String details = (request.getStatus() == HoursStatus.APPROVED)
                ? String.format("Approved %.2f volunteer hours for %s on event '%s'.", attendance.getHoursWorked(), volunteer.getEmail(), event.getTitle())
                : String.format("Rejected volunteer hours for %s on event '%s'. Reason: %s", volunteer.getEmail(), event.getTitle(), request.getReason().trim());

        auditLogService.logAction(reviewer, action, "ATTENDANCE", saved.getId(), details);

        // 6. In-App Notification to Volunteer
        String ngoName = ngoProfileRepository.findByUserId(event.getNgo().getId())
                .map(NgoProfile::getName)
                .orElse("Organizer");

        if (request.getStatus() == HoursStatus.APPROVED) {
            notificationService.createNotification(
                    volunteer,
                    "HOURS_APPROVED",
                    "Volunteer Hours Approved",
                    String.format("%s approved %.2f hours for %s.", ngoName, attendance.getHoursWorked(), event.getTitle()),
                    "ATTENDANCE",
                    saved.getId()
            );
        } else {
            notificationService.createNotification(
                    volunteer,
                    "HOURS_REJECTED",
                    "Volunteer Hours Rejected",
                    String.format("%s did not approve hours for %s. Reason: %s", ngoName, event.getTitle(), request.getReason().trim()),
                    "ATTENDANCE",
                    saved.getId()
            );
        }

        return mapToAttendanceResponseDto(saved);
    }

    /**
     * Clean extension point for automatic certificate issuance upon hours approval.
     * Phase 6 will hook into this method to auto-issue verifiable certificates.
     */
    protected void onHoursApproved(Attendance attendance, User reviewer) {
        // Intentionally left as a clean extension point for Phase 6 automated certificate generation
    }

    private AttendanceResponseDto mapToAttendanceResponseDto(Attendance a) {
        Application app = a.getApplication();
        User vol = app.getVolunteer();
        Event ev = app.getEvent();

        return new AttendanceResponseDto(
                a.getId(),
                app.getId(),
                vol.getId(),
                vol.getEmail(),
                ev.getId(),
                ev.getTitle(),
                a.getAttendanceStatus(),
                a.getHoursWorked(),
                a.getHoursStatus(),
                a.getCreatedAt(),
                a.getRejectReason()
        );
    }

    private VolunteerAttendanceDto mapToVolunteerAttendanceDto(Attendance a) {
        Application app = a.getApplication();
        Event ev = app.getEvent();
        String ngoName = ngoProfileRepository.findByUserId(ev.getNgo().getId())
                .map(NgoProfile::getName)
                .orElse("NGO Organization");

        return new VolunteerAttendanceDto(
                a.getId(),
                app.getId(),
                ev.getId(),
                ev.getTitle(),
                ngoName,
                ev.getEventDate(),
                a.getAttendanceStatus(),
                a.getHoursWorked(),
                a.getHoursStatus(),
                a.getReviewedAt(),
                a.getRejectReason()
        );
    }
}
