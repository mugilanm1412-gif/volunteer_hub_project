package com.volunteerhub;

import com.volunteerhub.entity.VolunteerRating;
import com.volunteerhub.service.PerformanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PerformanceCalculator Pure Logic Unit Tests")
class PerformanceCalculatorTests {

    private PerformanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PerformanceCalculator();
    }

    @Test
    @DisplayName("Attendance Rate: calculates correctly and handles zero completed events")
    void testAttendanceRate() {
        assertEquals(0.0, calculator.calculateAttendanceRate(0, 0), "Zero completed events must yield 0.0%");
        assertEquals(0.0, calculator.calculateAttendanceRate(0, 5), "Zero attended out of 5 completed yields 0.0%");
        assertEquals(60.0, calculator.calculateAttendanceRate(3, 5), "3 attended out of 5 completed yields 60.0%");
        assertEquals(100.0, calculator.calculateAttendanceRate(5, 5), "5 attended out of 5 completed yields 100.0%");
        assertEquals(100.0, calculator.calculateAttendanceRate(6, 5), "Should cap at 100.0%");
    }

    @Test
    @DisplayName("Hours Score: min(hours / 100, 1.0) * 100 and capping above 100 hours")
    void testHoursScore() {
        assertEquals(0.0, calculator.calculateHoursScore(null), "Null hours yields 0.0");
        assertEquals(0.0, calculator.calculateHoursScore(BigDecimal.ZERO), "Zero hours yields 0.0");
        assertEquals(25.0, calculator.calculateHoursScore(BigDecimal.valueOf(25)), "25 hours yields 25.0");
        assertEquals(99.5, calculator.calculateHoursScore(BigDecimal.valueOf(99.5)), "99.5 hours yields 99.5");
        assertEquals(100.0, calculator.calculateHoursScore(BigDecimal.valueOf(100)), "100 hours yields 100.0");
        assertEquals(100.0, calculator.calculateHoursScore(BigDecimal.valueOf(150)), "Hours above 100 caps at 100.0");
        assertEquals(100.0, calculator.calculateHoursScore(BigDecimal.valueOf(500)), "500 hours caps at 100.0");
    }

    @Test
    @DisplayName("Average Rating: arithmetic mean of punctuality, teamwork, and effort across all evaluations")
    void testAverageRating() {
        assertNull(calculator.calculateAverageRating(null), "Null ratings yields null");
        assertNull(calculator.calculateAverageRating(Collections.emptyList()), "Empty ratings yields null");

        VolunteerRating r1 = new VolunteerRating();
        r1.setPunctuality(5);
        r1.setTeamwork(4);
        r1.setEffort(3);
        // (5 + 4 + 3) / 3 = 4.0
        assertEquals(4.0, calculator.calculateAverageRating(List.of(r1)));

        VolunteerRating r2 = new VolunteerRating();
        r2.setPunctuality(5);
        r2.setTeamwork(5);
        r2.setEffort(5);
        // Total points = 12 + 15 = 27, metrics = 6 -> 27 / 6 = 4.5
        assertEquals(4.5, calculator.calculateAverageRating(List.of(r1, r2)));
    }

    @Test
    @DisplayName("Overall Score With Ratings: 40% hours + 30% attendance + 30% rating")
    void testOverallPerformanceScore_WithRatings() {
        // Perfect 100: 100 hours (100), 10/10 attendance (100), 5.0 rating (100) -> 100.0
        double perfectScore = calculator.calculateOverallPerformanceScore(BigDecimal.valueOf(100), 10, 10, 5.0);
        assertEquals(100.0, perfectScore);

        // Mixed: 50 hours (50 score), 4/5 attendance (80 score), 4.0 rating (80 score)
        // 0.40 * 50 + 0.30 * 80 + 0.30 * 80 = 20 + 24 + 24 = 68.0
        double mixedScore = calculator.calculateOverallPerformanceScore(BigDecimal.valueOf(50), 4, 5, 4.0);
        assertEquals(68.0, mixedScore);
    }

    @Test
    @DisplayName("Overall Score Without Ratings: Redistributes 30% weight proportionally (4/7 hours, 3/7 attendance)")
    void testOverallPerformanceScore_NoRatings_Redistribution() {
        // Equal 70 score: 70 hours (70), 7/10 attendance (70), no rating -> (4/7)*70 + (3/7)*70 = 70.0
        double score70 = calculator.calculateOverallPerformanceScore(BigDecimal.valueOf(70), 7, 10, null);
        assertEquals(70.0, score70);

        // 100 hours (100), 0 attendance (0) -> (4/7) * 100 = 57.14
        double hoursOnly = calculator.calculateOverallPerformanceScore(BigDecimal.valueOf(100), 0, 5, null);
        assertEquals(57.14, hoursOnly);

        // 0 hours (0), 100% attendance (100) -> (3/7) * 100 = 42.86
        double attOnly = calculator.calculateOverallPerformanceScore(BigDecimal.ZERO, 5, 5, null);
        assertEquals(42.86, attOnly);
    }

    @Test
    @DisplayName("Overall Score: Zero events and zero hours yields 0.0")
    void testOverallPerformanceScore_ZeroEvents() {
        double zeroScore = calculator.calculateOverallPerformanceScore(BigDecimal.ZERO, 0, 0, null);
        assertEquals(0.0, zeroScore);
    }

    @Test
    @DisplayName("Badge Boundary 39 vs 40: Score < 40 is NEWCOMER, Score >= 40 with 1 event is BRONZE")
    void testBadgeBoundary_39_vs_40() {
        // Score 39.99 with hours and event -> NEWCOMER
        assertEquals(PerformanceCalculator.BADGE_NEWCOMER,
                calculator.determineBadge(39.99, BigDecimal.valueOf(15), 2));

        // Score 40.0 with hours and 1 event -> BRONZE
        assertEquals(PerformanceCalculator.BADGE_BRONZE,
                calculator.determineBadge(40.0, BigDecimal.valueOf(10), 1));

        // Score 40.0 with 0 hours -> NEWCOMER
        assertEquals(PerformanceCalculator.BADGE_NEWCOMER,
                calculator.determineBadge(40.0, BigDecimal.ZERO, 1));
    }

    @Test
    @DisplayName("Badge Boundary 64 vs 65: Score < 65 is BRONZE, Score >= 65 with >=20h & >=3 events is SILVER")
    void testBadgeBoundary_64_vs_65() {
        // Score 64.99 with 30h and 5 events -> BRONZE
        assertEquals(PerformanceCalculator.BADGE_BRONZE,
                calculator.determineBadge(64.99, BigDecimal.valueOf(30), 5));

        // Score 65.0 with 20h and 3 events -> SILVER
        assertEquals(PerformanceCalculator.BADGE_SILVER,
                calculator.determineBadge(65.0, BigDecimal.valueOf(20), 3));

        // Score 65.0 but only 19.9h -> BRONZE
        assertEquals(PerformanceCalculator.BADGE_BRONZE,
                calculator.determineBadge(65.0, BigDecimal.valueOf(19.9), 3));

        // Score 65.0 but only 2 events -> BRONZE
        assertEquals(PerformanceCalculator.BADGE_BRONZE,
                calculator.determineBadge(65.0, BigDecimal.valueOf(25), 2));
    }

    @Test
    @DisplayName("Badge Boundary 84 vs 85: Score < 85 is SILVER, Score >= 85 with >=50h & >=6 events is GOLD")
    void testBadgeBoundary_84_vs_85() {
        // Score 84.99 with 80h and 10 events -> SILVER
        assertEquals(PerformanceCalculator.BADGE_SILVER,
                calculator.determineBadge(84.99, BigDecimal.valueOf(80), 10));

        // Score 85.0 with 50h and 6 events -> GOLD
        assertEquals(PerformanceCalculator.BADGE_GOLD,
                calculator.determineBadge(85.0, BigDecimal.valueOf(50), 6));

        // Score 85.0 but only 49.9h -> SILVER
        assertEquals(PerformanceCalculator.BADGE_SILVER,
                calculator.determineBadge(85.0, BigDecimal.valueOf(49.9), 6));

        // Score 85.0 but only 5 events -> SILVER
        assertEquals(PerformanceCalculator.BADGE_SILVER,
                calculator.determineBadge(85.0, BigDecimal.valueOf(60), 5));
    }
}
