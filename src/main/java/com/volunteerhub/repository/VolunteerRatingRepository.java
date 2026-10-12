package com.volunteerhub.repository;

import com.volunteerhub.entity.VolunteerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing VolunteerRating entities.
 */
@Repository
public interface VolunteerRatingRepository extends JpaRepository<VolunteerRating, Long> {

    Optional<VolunteerRating> findByAttendanceId(Long attendanceId);

    boolean existsByAttendanceId(Long attendanceId);

    /**
     * Retrieves all performance ratings awarded to a specific volunteer with fetch joins to avoid N+1 queries.
     */
    @Query("SELECT r FROM VolunteerRating r " +
           "JOIN FETCH r.attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.volunteer v " +
           "JOIN FETCH app.event e " +
           "JOIN FETCH r.ratedBy u " +
           "WHERE v.id = :volunteerId " +
           "ORDER BY r.createdAt DESC")
    List<VolunteerRating> findByVolunteerId(@Param("volunteerId") Long volunteerId);
}
