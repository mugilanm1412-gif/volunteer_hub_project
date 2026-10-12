package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service orchestrating role-scoped consolidated dashboard telemetry, feeds, and analytics.
 * Strictly enforces aggregate database queries to ensure zero N+1 latency.
 */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final EventRepository eventRepository;
    private final ApplicationRepository applicationRepository;
    private final AttendanceRepository attendanceRepository;
    private final CertificateRepository certificateRepository;
    private final VolunteerRatingRepository volunteerRatingRepository;
    private final PerformanceService performanceService;
    private final ReportService reportService;

    public DashboardService(UserRepository userRepository,
                            VolunteerProfileRepository volunteerProfileRepository,
                            NgoProfileRepository ngoProfileRepository,
                            EventRepository eventRepository,
                            ApplicationRepository applicationRepository,
                            AttendanceRepository attendanceRepository,
                            CertificateRepository certificateRepository,
                            VolunteerRatingRepository volunteerRatingRepository,
                            PerformanceService performanceService,
                            ReportService reportService) {
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.eventRepository = eventRepository;
        this.applicationRepository = applicationRepository;
        this.attendanceRepository = attendanceRepository;
        this.certificateRepository = certificateRepository;
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.performanceService = performanceService;
        this.reportService = reportService;
    }

    /**
     * Builds consolidated telemetry for the authenticated volunteer.
     */
    @Transactional(readOnly = true)
    public VolunteerDashboardDto getVolunteerDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer user not found with email: " + email));

        VolunteerProfile profile = volunteerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user: " + email));

        Long volunteerId = user.getId();
        LocalDate today = LocalDate.now();

        // 1. Core Performance Metrics (calculated on-the-fly)
        VolunteerPerformanceDto performance = performanceService.getVolunteerPerformance(email);

        // 2. Upcoming Approved Events Feed
        Page<Application> upcomingAppsPage = applicationRepository.findUpcomingApplications(
                volunteerId, today, PageRequest.of(0, 5));

        List<VolunteerUpcomingEventDto> upcomingList = new ArrayList<>();
        for (Application app : upcomingAppsPage.getContent()) {
            Event ev = app.getEvent();
            String ngoName = (ev.getNgo() != null)
                    ? ngoProfileRepository.findByUserId(ev.getNgo().getId())
                    .map(NgoProfile::getName)
                    .orElse(ev.getNgo().getEmail())
                    : "Community Organization";

            upcomingList.add(new VolunteerUpcomingEventDto(
                    ev.getId(),
                    ev.getTitle(),
                    ev.getCategory(),
                    ev.getEventDate(),
                    ev.getStartTime(),
                    ev.getEndTime(),
                    ev.getLocation(),
                    ngoName
            ));
        }

        // 3. Counts
        int upcomingCount = (int) upcomingAppsPage.getTotalElements();
        int pendingCount = (int) applicationRepository.countByVolunteerIdAndStatus(volunteerId, ApplicationStatus.PENDING);
        int certificatesCount = (int) certificateRepository.countByVolunteerId(volunteerId);

        // 4. Recent Activity Timeline
        List<Attendance> attendances = attendanceRepository.findByVolunteerIdWithDetails(volunteerId);
        List<RecentActivityItemDto> recentActivity = new ArrayList<>();
        int limit = Math.min(attendances.size(), 5);

        for (int i = 0; i < limit; i++) {
            Attendance att = attendances.get(i);
            Event ev = att.getApplication().getEvent();
            Double ratingVal = volunteerRatingRepository.findByAttendanceId(att.getId())
                    .map(r -> (r.getPunctuality() + r.getTeamwork() + r.getEffort()) / 3.0)
                    .orElse(null);

            recentActivity.add(new RecentActivityItemDto(
                    ev.getId(),
                    ev.getTitle(),
                    ev.getEventDate(),
                    att.getHoursWorked() != null ? att.getHoursWorked() : BigDecimal.ZERO,
                    att.getHoursStatus().name(),
                    ratingVal
            ));
        }

        // 5. Category distribution for volunteer
        List<CategoryHoursProjection> catProjections = attendanceRepository.findCategoryHoursByVolunteerId(volunteerId);
        List<CategoryHoursDto> categoryHours = new ArrayList<>();
        for (CategoryHoursProjection p : catProjections) {
            categoryHours.add(new CategoryHoursDto(
                    p.getCategory(),
                    p.getTotalHours() != null ? p.getTotalHours() : BigDecimal.ZERO,
                    p.getEventCount() != null ? p.getEventCount() : 0L,
                    p.getVolunteerCount() != null ? p.getVolunteerCount() : 0L
            ));
        }

        return new VolunteerDashboardDto(
                profile.getFullName(),
                performance.getTotalVerifiedHours(),
                performance.getCompletedEvents(),
                upcomingList,
                upcomingCount,
                pendingCount,
                performance.getAttendanceRate(),
                performance.getAverageRating(),
                performance.getPerformanceScore(),
                performance.getBadge() != null ? performance.getBadge() : "BRONZE",
                certificatesCount,
                recentActivity,
                categoryHours
        );
    }

    /**
     * Builds consolidated telemetry and activity graphs for the authenticated NGO.
     */
    @Transactional(readOnly = true)
    public NgoDashboardDto getNgoDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("NGO user not found with email: " + email));

        NgoProfile profile = ngoProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user: " + email));

        Long ngoId = user.getId();
        LocalDate today = LocalDate.now();

        long eventsCreated = eventRepository.countByNgoId(ngoId);
        long upcomingEvents = eventRepository.countByNgoIdAndStatusAndEventDateGreaterThanEqual(
                ngoId, EventStatus.PUBLISHED, today);
        long completedEvents = eventRepository.countByNgoIdAndStatus(ngoId, EventStatus.COMPLETED);
        long totalApplicants = applicationRepository.countByEventNgoId(ngoId);
        long approvedVolunteers = applicationRepository.countByEventNgoIdAndStatus(ngoId, ApplicationStatus.APPROVED);
        BigDecimal verifiedHours = attendanceRepository.sumVerifiedHoursByNgoId(ngoId);
        if (verifiedHours == null) {
            verifiedHours = BigDecimal.ZERO;
        }

        List<CategoryHoursDto> hoursByCategory = reportService.getHoursByCategory(ngoId);
        List<ActivityDataPointDto> activity = reportService.getActivityReport("MONTHLY", ngoId);

        return new NgoDashboardDto(
                profile.getName(),
                profile.getVerificationStatus().name(),
                eventsCreated,
                upcomingEvents,
                completedEvents,
                totalApplicants,
                approvedVolunteers,
                verifiedHours,
                hoursByCategory,
                activity
        );
    }

    /**
     * Builds platform-wide governance dashboard metrics and visual analytics for administrators.
     */
    @Transactional(readOnly = true)
    public AdminDashboardDto getAdminDashboard() {
        long totalUsers = userRepository.count();
        long totalVolunteers = userRepository.countByRole(Role.VOLUNTEER);
        long verifiedNgos = ngoProfileRepository.countByVerificationStatus(NgoVerificationStatus.VERIFIED);
        long pendingNgos = ngoProfileRepository.countByVerificationStatus(NgoVerificationStatus.PENDING);
        long activeEvents = eventRepository.countByStatus(EventStatus.PUBLISHED);
        long completedEvents = eventRepository.countByStatus(EventStatus.COMPLETED);
        BigDecimal totalVerifiedHours = attendanceRepository.sumTotalVerifiedHours();
        if (totalVerifiedHours == null) {
            totalVerifiedHours = BigDecimal.ZERO;
        }

        long recentRegistrations = applicationRepository.count();
        List<CategoryHoursDto> hoursByCategory = reportService.getHoursByCategory(null);
        List<ActivityDataPointDto> activity = reportService.getActivityReport("MONTHLY", null);

        return new AdminDashboardDto(
                totalUsers,
                totalVolunteers,
                verifiedNgos,
                pendingNgos,
                activeEvents,
                completedEvents,
                totalVerifiedHours,
                recentRegistrations,
                hoursByCategory,
                activity
        );
    }
}
