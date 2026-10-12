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
}
