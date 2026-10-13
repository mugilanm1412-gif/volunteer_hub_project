package com.volunteerhub.service;

import com.volunteerhub.dto.*;
import com.volunteerhub.entity.Attendance;
import com.volunteerhub.entity.AttendanceStatus;
import com.volunteerhub.entity.HoursStatus;
import com.volunteerhub.entity.VolunteerProfile;
import com.volunteerhub.entity.VolunteerRating;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.ApplicationRepository;
import com.volunteerhub.repository.AttendanceRepository;
import com.volunteerhub.repository.VolunteerProfileRepository;
import com.volunteerhub.repository.VolunteerRatingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;

/**
 * Service for computing dynamic volunteer performance summaries, progress charts,
 * privacy-preserving leaderboard rankings, and official OpenPDF reports.
 */
@Service
public class PerformanceService {

    private final VolunteerProfileRepository volunteerProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationRepository applicationRepository;
    private final VolunteerRatingRepository volunteerRatingRepository;
    private final PerformanceCalculator performanceCalculator;
    private final PdfReportService pdfReportService;

    public PerformanceService(VolunteerProfileRepository volunteerProfileRepository,
                              AttendanceRepository attendanceRepository,
                              ApplicationRepository applicationRepository,
                              VolunteerRatingRepository volunteerRatingRepository,
                              PerformanceCalculator performanceCalculator,
                              PdfReportService pdfReportService) {
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.attendanceRepository = attendanceRepository;
        this.applicationRepository = applicationRepository;
        this.volunteerRatingRepository = volunteerRatingRepository;
        this.performanceCalculator = performanceCalculator;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Dynamically calculates performance score, badge, hours, attendance rate, and average rating
     * for the authenticated volunteer. Zero computed metrics are persisted in table columns.
     */
    @Transactional(readOnly = true)
    public VolunteerPerformanceDto getVolunteerPerformance(String volunteerEmail) {
        VolunteerProfile profile = getProfileByEmail(volunteerEmail);
        Long volunteerUserId = profile.getUser().getId();

        List<Attendance> attendances = attendanceRepository.findByVolunteerIdWithDetails(volunteerUserId);
        List<VolunteerRating> ratings = volunteerRatingRepository.findByVolunteerId(volunteerUserId);

        // Sum approved hours worked
        BigDecimal totalVerifiedHours = attendances.stream()
                .filter(a -> a.getHoursStatus() == HoursStatus.APPROVED && a.getHoursWorked() != null)
                .map(a -> a.getHoursWorked())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // Count attended events (PRESENT)
        int completedEvents = (int) attendances.stream()
                .filter(a -> a.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();

        // Count approved applications for events that have completed
        int registeredCompletedEvents = applicationRepository.countCompletedApplications(volunteerUserId, LocalDate.now());
        if (registeredCompletedEvents < completedEvents) {
            registeredCompletedEvents = completedEvents;
        }

        double attendanceRate = performanceCalculator.calculateAttendanceRate(completedEvents, registeredCompletedEvents);
        Double averageRating = performanceCalculator.calculateAverageRating(ratings);

        double hoursScore = performanceCalculator.calculateHoursScore(totalVerifiedHours);
        double attendanceScore = attendanceRate;
        Double ratingScore = performanceCalculator.calculateRatingScore(averageRating);

        double performanceScore = performanceCalculator.calculateOverallPerformanceScore(
                totalVerifiedHours, completedEvents, registeredCompletedEvents, averageRating);

        String badge = performanceCalculator.determineBadge(performanceScore, totalVerifiedHours, completedEvents);

        return new VolunteerPerformanceDto(
                totalVerifiedHours,
                completedEvents,
                registeredCompletedEvents,
                attendanceRate,
                averageRating,
                hoursScore,
                attendanceScore,
                ratingScore,
                performanceScore,
                badge,
                profile.isShowOnLeaderboard()
        );
    }

    /**
     * Builds chronological time-series of approved service hours grouped by week or month.
     */
    @Transactional(readOnly = true)
    public List<ProgressPointDto> getProgress(String volunteerEmail, String granularity) {
        VolunteerProfile profile = getProfileByEmail(volunteerEmail);
        Long volunteerUserId = profile.getUser().getId();

        List<Attendance> attendances = attendanceRepository.findByVolunteerIdWithDetails(volunteerUserId);

        boolean isWeekly = "week".equalsIgnoreCase(granularity);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, BigDecimal> groupedHours = new TreeMap<>();

        for (Attendance att : attendances) {
            if (att.getHoursStatus() == HoursStatus.APPROVED && att.getHoursWorked() != null
                    && att.getHoursWorked().compareTo(BigDecimal.ZERO) > 0) {
                LocalDate eventDate = att.getApplication().getEvent().getEventDate();
                String periodKey;
                if (isWeekly) {
                    int weekYear = eventDate.get(IsoFields.WEEK_BASED_YEAR);
                    int weekNum = eventDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    periodKey = String.format("%d-W%02d", weekYear, weekNum);
                } else {
                    periodKey = eventDate.format(monthFormatter);
                }

                groupedHours.merge(periodKey, att.getHoursWorked(), (existingHours, newHours) -> existingHours.add(newHours));
            }
        }

        List<ProgressPointDto> progressPoints = new ArrayList<>();
        groupedHours.forEach((period, hours) -> progressPoints.add(new ProgressPointDto(period, hours)));
        return progressPoints;
    }

    /**
     * Updates volunteer preference for inclusion in public leaderboard rankings.
     */
    @Transactional
    public Map<String, Object> updateLeaderboardVisibility(String volunteerEmail, LeaderboardVisibilityRequest request) {
        VolunteerProfile profile = getProfileByEmail(volunteerEmail);
        profile.setShowOnLeaderboard(request.getShowOnLeaderboard());
        volunteerProfileRepository.save(profile);

        Map<String, Object> response = new HashMap<>();
        response.put("showOnLeaderboard", profile.isShowOnLeaderboard());
        response.put("message", "Leaderboard visibility preference updated successfully.");
        return response;
    }

    /**
     * Retrieves paginated public leaderboard entries.
     *
     * How memory overflow is avoided:
     * 1. Uses a database-level native aggregate projection query (`VolunteerProfileRepository.findLeaderboardPage`).
     * 2. SQL LIMIT and OFFSET restrict loaded records strictly to the requested page size (e.g. 10 rows).
     * 3. Zero full JPA entity instances (User, VolunteerProfile, Attendance, etc.) are materialized in JVM memory.
     * 4. Excludes volunteers who opted out (show_on_leaderboard = FALSE) and inactive accounts at the DB level.
     * 5. Strictly protects volunteer privacy by returning only displayName, score, badge, verifiedHours, and rank.
     */
    @Transactional(readOnly = true)
    public PageResponseDto<LeaderboardEntryDto> getLeaderboard(Pageable pageable) {
        // Enforce pageable without conflicting sorts since sorting is performed by score & hours in the native query
        Pageable queryPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<LeaderboardRowProjection> projectionPage = volunteerProfileRepository.findLeaderboardPage(queryPageable);

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int startingRank = (int) queryPageable.getOffset() + 1;

        List<LeaderboardRowProjection> content = projectionPage.getContent();
        for (int i = 0; i < content.size(); i++) {
            LeaderboardRowProjection row = content.get(i);
            int rank = startingRank + i;
            double score = row.getScore() != null ? row.getScore() : 0.0;
            BigDecimal verifiedHours = row.getVerifiedHours() != null ? row.getVerifiedHours() : BigDecimal.ZERO;
            String badge = performanceCalculator.determineBadge(score, verifiedHours, row.getAttendedEvents());

            entries.add(new LeaderboardEntryDto(rank, row.getDisplayName(), score, badge, verifiedHours));
        }

        return new PageResponseDto<>(
                entries,
                projectionPage.getNumber(),
                projectionPage.getSize(),
                projectionPage.getTotalElements(),
                projectionPage.getTotalPages(),
                projectionPage.isLast()
        );
    }

    /**
     * Generates a verified, styled PDF performance report document for the volunteer.
     */
    @Transactional(readOnly = true)
    public byte[] generatePerformancePdf(String volunteerEmail) {
        VolunteerProfile profile = getProfileByEmail(volunteerEmail);
        Long volunteerUserId = profile.getUser().getId();

        VolunteerPerformanceDto performance = getVolunteerPerformance(volunteerEmail);
        List<Attendance> attendances = attendanceRepository.findByVolunteerIdWithDetails(volunteerUserId);
        List<VolunteerRating> ratings = volunteerRatingRepository.findByVolunteerId(volunteerUserId);

        return pdfReportService.generatePerformancePdf(profile, performance, attendances, ratings);
    }

    private VolunteerProfile getProfileByEmail(String email) {
        return volunteerProfileRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user: " + email));
    }
}
