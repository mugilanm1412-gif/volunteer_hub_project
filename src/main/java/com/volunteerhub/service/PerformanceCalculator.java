package com.volunteerhub.service;

import com.volunteerhub.entity.VolunteerRating;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure calculation engine for volunteer performance metrics, scores, and badges.
 * Contains pure mathematical logic with zero database dependencies.
 *
 * Formulas strictly adhere to Section 5.4 of the architecture specifications:
 *  - Attendance Rate: Attended events (PRESENT) / Registered completed events
 *  - Average Rating: Mean of punctuality, teamwork, and effort across all evaluations (1 to 5)
 *  - Hours Score: min(verifiedHours / 100, 1.0) * 100
 *  - Rating Score: (averageRating / 5.0) * 100
 *  - Performance Score: 40% hours + 30% attendance + 30% rating (with proportional redistribution if unrated)
 *  - Tiered Badges: GOLD (>=85, >=50h, >=6 events), SILVER (>=65, >=20h, >=3 events), BRONZE (>=40, >=1 event), NEWCOMER
 */
@Component
public class PerformanceCalculator {

    // Badge requirement thresholds as named constants
    public static final double GOLD_SCORE_THRESHOLD = 85.0;
    public static final BigDecimal GOLD_HOURS_THRESHOLD = BigDecimal.valueOf(50);
    public static final int GOLD_EVENTS_THRESHOLD = 6;

    public static final double SILVER_SCORE_THRESHOLD = 65.0;
    public static final BigDecimal SILVER_HOURS_THRESHOLD = BigDecimal.valueOf(20);
    public static final int SILVER_EVENTS_THRESHOLD = 3;

    public static final double BRONZE_SCORE_THRESHOLD = 40.0;
    public static final int BRONZE_EVENTS_THRESHOLD = 1;

    public static final String BADGE_GOLD = "GOLD";
    public static final String BADGE_SILVER = "SILVER";
    public static final String BADGE_BRONZE = "BRONZE";
    public static final String BADGE_NEWCOMER = "NEWCOMER";

    // Weight constants for performance score calculation
    public static final double WEIGHT_HOURS = 0.40;
    public static final double WEIGHT_ATTENDANCE = 0.30;
    public static final double WEIGHT_RATING = 0.30;

    /**
     * Calculates attendance rate percentage = (attendedEvents / registeredCompletedEvents) * 100.
     *
     * @param attendedEvents count of events with attendanceStatus = PRESENT
     * @param registeredCompletedEvents count of approved registrations for events that have completed
     * @return attendance rate percentage between 0.0 and 100.0
     */
    public double calculateAttendanceRate(int attendedEvents, int registeredCompletedEvents) {
        if (registeredCompletedEvents <= 0 || attendedEvents <= 0) {
            return 0.0;
        }
        double rate = ((double) attendedEvents / (double) registeredCompletedEvents) * 100.0;
        return Math.min(100.0, Math.max(0.0, rate));
    }

    /**
     * Calculates the unweighted arithmetic mean rating across all punctuality, teamwork, and effort evaluations.
     * Each evaluation has 3 scores (1-5), so the overall average is the sum of all individual scores divided by (count * 3).
     *
     * @param ratings list of VolunteerRating entities
     * @return average rating on 1.0 - 5.0 scale, or null if no ratings exist
     */
    public Double calculateAverageRating(List<VolunteerRating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return null;
        }
        long totalPoints = 0;
        int totalMetrics = 0;

        for (VolunteerRating r : ratings) {
            if (r != null && r.getPunctuality() != null && r.getTeamwork() != null && r.getEffort() != null) {
                totalPoints += r.getPunctuality() + r.getTeamwork() + r.getEffort();
                totalMetrics += 3;
            }
        }

        if (totalMetrics == 0) {
            return null;
        }

        double avg = (double) totalPoints / (double) totalMetrics;
        return roundToTwoDecimals(avg);
    }

    /**
     * Calculates hours score on a 0 to 100 scale: min(verifiedHours / 100, 1.0) * 100.
     * Verified hours are capped at 100 hours.
     *
     * @param verifiedHours total verified service hours
     * @return score between 0.0 and 100.0
     */
    public double calculateHoursScore(BigDecimal verifiedHours) {
        if (verifiedHours == null || verifiedHours.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        double hours = verifiedHours.doubleValue();
        return Math.min(hours / 100.0, 1.0) * 100.0;
    }

    /**
     * Calculates rating score on a 0 to 100 scale: (averageRating / 5.0) * 100.
     *
     * @param averageRating average rating between 1.0 and 5.0
     * @return score between 0.0 and 100.0, or null if volunteer has no ratings
     */
    public Double calculateRatingScore(Double averageRating) {
        if (averageRating == null) {
            return null;
        }
        double score = (averageRating / 5.0) * 100.0;
        return Math.min(100.0, Math.max(0.0, score));
    }

    /**
     * Calculates the overall composite performance score on a 0 to 100 scale.
     *
     * Standard formula:
     *   Score = (0.40 * hoursScore) + (0.30 * attendanceScore) + (0.30 * ratingScore)
     *
     * When volunteer has NO ratings:
     *   The 30% rating weight is redistributed proportionally to hours and attendance.
     *   Total weight remaining = 40% + 30% = 70%.
     *   Redistributed Hours Weight = 40 / 70 = 4/7 (~57.142857%)
     *   Redistributed Attendance Weight = 30 / 70 = 3/7 (~42.857143%)
     *   Sum = 4/7 + 3/7 = 7/7 = 100%.
     *   Score = (4/7 * hoursScore) + (3/7 * attendanceScore)
     *
     * @param verifiedHours total approved hours
     * @param attendedEvents count of events attended
     * @param registeredCompletedEvents count of completed events applied and approved for
     * @param averageRating average rating (1.0 to 5.0) or null if unrated
     * @return overall score rounded to 2 decimal places, bounded between 0.0 and 100.0
     */
    public double calculateOverallPerformanceScore(BigDecimal verifiedHours,
                                                   int attendedEvents,
                                                   int registeredCompletedEvents,
                                                   Double averageRating) {
        double hoursScore = calculateHoursScore(verifiedHours);
        double attendanceScore = calculateAttendanceRate(attendedEvents, registeredCompletedEvents);

        double score;
        if (averageRating != null) {
            Double ratingScore = calculateRatingScore(averageRating);
            score = (WEIGHT_HOURS * hoursScore) +
                    (WEIGHT_ATTENDANCE * attendanceScore) +
                    (WEIGHT_RATING * ratingScore);
        } else {
            // Proportional redistribution of the 30% rating weight across hours (40%) and attendance (30%):
            // Hours receives 40 / (40 + 30) = 4/7
            // Attendance receives 30 / (40 + 30) = 3/7
            double redistributedHoursWeight = 4.0 / 7.0;
            double redistributedAttendanceWeight = 3.0 / 7.0;
            score = (redistributedHoursWeight * hoursScore) + (redistributedAttendanceWeight * attendanceScore);
        }

        double boundedScore = Math.min(100.0, Math.max(0.0, score));
        return roundToTwoDecimals(boundedScore);
    }

    /**
     * Determines badge level based on composite score, total verified hours, and attended events.
     *
     * Thresholds:
     *  - Gold: score >= 85.0 && verifiedHours >= 50 && attendedEvents >= 6
     *  - Silver: score >= 65.0 && verifiedHours >= 20 && attendedEvents >= 3
     *  - Bronze: score >= 40.0 && attendedEvents >= 1 (with approved hours)
     *  - Below Bronze: NEWCOMER
     *
     * @param score overall performance score (0 to 100)
     * @param verifiedHours verified hours accumulated
     * @param attendedEvents count of events with confirmed attendance
     * @return Badge string: "GOLD", "SILVER", "BRONZE", or "NEWCOMER"
     */
    public String determineBadge(double score, BigDecimal verifiedHours, int attendedEvents) {
        BigDecimal hours = (verifiedHours != null) ? verifiedHours : BigDecimal.ZERO;

        if (score >= GOLD_SCORE_THRESHOLD &&
            hours.compareTo(GOLD_HOURS_THRESHOLD) >= 0 &&
            attendedEvents >= GOLD_EVENTS_THRESHOLD) {
            return BADGE_GOLD;
        }

        if (score >= SILVER_SCORE_THRESHOLD &&
            hours.compareTo(SILVER_HOURS_THRESHOLD) >= 0 &&
            attendedEvents >= SILVER_EVENTS_THRESHOLD) {
            return BADGE_SILVER;
        }

        if (score >= BRONZE_SCORE_THRESHOLD &&
            attendedEvents >= BRONZE_EVENTS_THRESHOLD &&
            hours.compareTo(BigDecimal.ZERO) > 0) {
            return BADGE_BRONZE;
        }

        return BADGE_NEWCOMER;
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
