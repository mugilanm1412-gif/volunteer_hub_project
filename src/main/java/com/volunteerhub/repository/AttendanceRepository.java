package com.volunteerhub.repository;

import com.volunteerhub.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Attendance entities and querying service records.
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Checks if an attendance record already exists for the given application.
     */
    boolean existsByApplicationId(Long applicationId);

    /**
     * Finds attendance for an application with eager association fetching.
     */
    Optional<Attendance> findByApplicationId(Long applicationId);

    /**
     * Retrieves all attendance records for an event with fetch joins to avoid N+1 queries.
     */
    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.volunteer v " +
           "JOIN FETCH app.event e " +
           "WHERE e.id = :eventId " +
           "ORDER BY a.createdAt ASC")
    List<Attendance> findByEventIdWithDetails(@Param("eventId") Long eventId);

    /**
     * Retrieves all attendance records for a volunteer with eager joins on application and event.
     */
    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.event e " +
           "JOIN FETCH e.ngo ngo " +
           "WHERE app.volunteer.id = :volunteerId " +
           "ORDER BY e.eventDate DESC")
    List<Attendance> findByVolunteerIdWithDetails(@Param("volunteerId") Long volunteerId);

    /**
     * Retrieves an attendance record by ID with complete eager associations.
     */
    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.volunteer v " +
           "JOIN FETCH app.event e " +
           "LEFT JOIN FETCH e.ngo ngo " +
           "WHERE a.id = :id")
    Optional<Attendance> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM Attendance a WHERE a.hoursStatus = 'APPROVED'")
    java.math.BigDecimal sumTotalVerifiedHours();

    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM Attendance a " +
           "JOIN a.application app " +
           "JOIN app.event e " +
           "WHERE a.hoursStatus = 'APPROVED' " +
           "AND e.ngo.id = :ngoId")
    java.math.BigDecimal sumVerifiedHoursByNgoId(@Param("ngoId") Long ngoId);

    @Query("SELECT e.category as category, " +
           "COALESCE(SUM(a.hoursWorked), 0) as totalHours, " +
           "COUNT(DISTINCT e.id) as eventCount, " +
           "COUNT(DISTINCT app.volunteer.id) as volunteerCount " +
           "FROM Attendance a " +
           "JOIN a.application app " +
           "JOIN app.event e " +
           "WHERE a.hoursStatus = 'APPROVED' " +
           "AND (:ngoId IS NULL OR e.ngo.id = :ngoId) " +
           "GROUP BY e.category " +
           "ORDER BY SUM(a.hoursWorked) DESC")
    List<com.volunteerhub.dto.CategoryHoursProjection> findCategoryHours(@Param("ngoId") Long ngoId);

    @Query("SELECT e.category as category, " +
           "COALESCE(SUM(a.hoursWorked), 0) as totalHours, " +
           "COUNT(DISTINCT e.id) as eventCount, " +
           "COUNT(DISTINCT app.volunteer.id) as volunteerCount " +
           "FROM Attendance a " +
           "JOIN a.application app " +
           "JOIN app.event e " +
           "WHERE a.hoursStatus = 'APPROVED' " +
           "AND app.volunteer.id = :volunteerId " +
           "GROUP BY e.category " +
           "ORDER BY SUM(a.hoursWorked) DESC")
    List<com.volunteerhub.dto.CategoryHoursProjection> findCategoryHoursByVolunteerId(@Param("volunteerId") Long volunteerId);

    @Query(value = "SELECT DATE_FORMAT(e.event_date, '%Y-%m') AS period, " +
                   "DATE_FORMAT(e.event_date, '%b %Y') AS label, " +
                   "COALESCE(SUM(a.hours_worked), 0) AS totalHours, " +
                   "COUNT(DISTINCT e.id) AS eventCount, " +
                   "COUNT(DISTINCT app.volunteer_id) AS volunteerCount " +
                   "FROM attendance a " +
                   "JOIN applications app ON a.application_id = app.id " +
                   "JOIN events e ON app.event_id = e.id " +
                   "WHERE a.hours_status = 'APPROVED' " +
                   "AND (:ngoId IS NULL OR e.ngo_id = :ngoId) " +
                   "GROUP BY period, label " +
                   "ORDER BY period ASC", nativeQuery = true)
    List<com.volunteerhub.dto.ActivityDataPointProjection> findMonthlyActivity(@Param("ngoId") Long ngoId);

    @Query(value = "SELECT DATE_FORMAT(e.event_date, '%Y-W%u') AS period, " +
                   "CONCAT('Week ', DATE_FORMAT(e.event_date, '%u, %Y')) AS label, " +
                   "COALESCE(SUM(a.hours_worked), 0) AS totalHours, " +
                   "COUNT(DISTINCT e.id) AS eventCount, " +
                   "COUNT(DISTINCT app.volunteer_id) AS volunteerCount " +
                   "FROM attendance a " +
                   "JOIN applications app ON a.application_id = app.id " +
                   "JOIN events e ON app.event_id = e.id " +
                   "WHERE a.hours_status = 'APPROVED' " +
                   "AND (:ngoId IS NULL OR e.ngo_id = :ngoId) " +
                   "GROUP BY period, label " +
                   "ORDER BY period ASC", nativeQuery = true)
    List<com.volunteerhub.dto.ActivityDataPointProjection> findWeeklyActivity(@Param("ngoId") Long ngoId);

    @Query(value = "SELECT DATE_FORMAT(e.event_date, '%Y-%m') AS period, " +
                   "DATE_FORMAT(e.event_date, '%b %Y') AS label, " +
                   "COALESCE(SUM(a.hours_worked), 0) AS totalHours, " +
                   "COUNT(DISTINCT e.id) AS eventCount, " +
                   "COUNT(DISTINCT app.volunteer_id) AS volunteerCount " +
                   "FROM attendance a " +
                   "JOIN applications app ON a.application_id = app.id " +
                   "JOIN events e ON app.event_id = e.id " +
                   "WHERE a.hours_status = 'APPROVED' " +
                   "AND app.volunteer_id = :volunteerId " +
                   "GROUP BY period, label " +
                   "ORDER BY period ASC", nativeQuery = true)
    List<com.volunteerhub.dto.ActivityDataPointProjection> findMonthlyActivityByVolunteerId(@Param("volunteerId") Long volunteerId);
}
