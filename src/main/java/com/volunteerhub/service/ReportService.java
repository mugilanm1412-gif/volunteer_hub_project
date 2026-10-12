package com.volunteerhub.service;

import com.volunteerhub.dto.ActivityDataPointDto;
import com.volunteerhub.dto.ActivityDataPointProjection;
import com.volunteerhub.dto.CategoryHoursDto;
import com.volunteerhub.dto.CategoryHoursProjection;
import com.volunteerhub.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing high-performance aggregated reporting for categories and activity timelines.
 * Uses native database groupings and projections to eliminate N+1 query overhead.
 */
@Service
public class ReportService {

    private final AttendanceRepository attendanceRepository;

    public ReportService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Aggregates verified volunteering hours and event/volunteer engagement grouped by category.
     *
     * @param ngoId optional NGO identifier to filter results; if null, aggregates across the platform
     * @return list of category hour summaries
     */
    @Transactional(readOnly = true)
    public List<CategoryHoursDto> getHoursByCategory(Long ngoId) {
        List<CategoryHoursProjection> projections = attendanceRepository.findCategoryHours(ngoId);
        List<CategoryHoursDto> dtos = new ArrayList<>();

        for (CategoryHoursProjection p : projections) {
            dtos.add(new CategoryHoursDto(
                    p.getCategory(),
                    p.getTotalHours() != null ? p.getTotalHours() : BigDecimal.ZERO,
                    p.getEventCount() != null ? p.getEventCount() : 0L,
                    p.getVolunteerCount() != null ? p.getVolunteerCount() : 0L
            ));
        }

        return dtos;
    }

    /**
     * Aggregates verified volunteering hours and participation counts across time periods.
     *
     * @param granularity "MONTHLY" (default) or "WEEKLY"
     * @param ngoId       optional NGO identifier to filter results
     * @return list of time-period activity summaries
     */
    @Transactional(readOnly = true)
    public List<ActivityDataPointDto> getActivityReport(String granularity, Long ngoId) {
        boolean isWeekly = granularity != null && granularity.trim().equalsIgnoreCase("WEEKLY");
        List<ActivityDataPointProjection> projections = isWeekly
                ? attendanceRepository.findWeeklyActivity(ngoId)
                : attendanceRepository.findMonthlyActivity(ngoId);

        List<ActivityDataPointDto> dtos = new ArrayList<>();
        for (ActivityDataPointProjection p : projections) {
            dtos.add(new ActivityDataPointDto(
                    p.getPeriod(),
                    p.getLabel(),
                    p.getTotalHours() != null ? p.getTotalHours() : BigDecimal.ZERO,
                    p.getEventCount() != null ? p.getEventCount() : 0L,
                    p.getVolunteerCount() != null ? p.getVolunteerCount() : 0L
            ));
        }

        return dtos;
    }
}
