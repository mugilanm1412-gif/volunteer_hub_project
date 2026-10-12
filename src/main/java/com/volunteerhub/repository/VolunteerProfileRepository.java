package com.volunteerhub.repository;

import com.volunteerhub.dto.LeaderboardRowProjection;
import com.volunteerhub.entity.VolunteerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for VolunteerProfile entity.
 * Includes memory-efficient database-level aggregated projection query for public leaderboard rankings.
 */
@Repository
public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, Long> {

    Optional<VolunteerProfile> findByUserId(Long userId);

    Optional<VolunteerProfile> findByUserEmail(String email);

    /**
     * Efficient database-level leaderboard projection query.
     * Computes verified hours, attendance counts, average ratings, and composite performance scores
     * directly in the MySQL database engine.
     *
     * Architectural Memory Efficiency:
     * 1. No JPA entity instances (VolunteerProfile, User, Application, Attendance, etc.) are loaded into JVM memory.
     * 2. SQL LIMIT and OFFSET (via Spring Data Pageable) strictly bound retrieved records to the requested page size (e.g., 10 rows).
     * 3. Aggregates and sort orders are resolved in the DB engine, achieving O(1) memory overhead regardless of total volunteers.
     */
    @Query(value = """
        SELECT 
            vp.user_id AS userId,
            vp.full_name AS displayName,
            COALESCE(SUM(CASE WHEN att.hours_status = 'APPROVED' THEN att.hours_worked ELSE 0 END), 0) AS verifiedHours,
            COUNT(DISTINCT CASE WHEN att.attendance_status = 'PRESENT' THEN att.id END) AS attendedEvents,
            COUNT(DISTINCT CASE WHEN app.status = 'APPROVED' AND (e.event_date < CURRENT_DATE OR e.status = 'COMPLETED') THEN app.id END) AS registeredEvents,
            AVG(CASE WHEN vr.id IS NOT NULL THEN (vr.punctuality + vr.teamwork + vr.effort) / 3.0 ELSE NULL END) AS avgRating,
            ROUND(
                CASE 
                    WHEN AVG(CASE WHEN vr.id IS NOT NULL THEN (vr.punctuality + vr.teamwork + vr.effort) / 3.0 ELSE NULL END) IS NOT NULL THEN
                        (0.40 * LEAST(COALESCE(SUM(CASE WHEN att.hours_status = 'APPROVED' THEN att.hours_worked ELSE 0 END), 0), 100.0))
                        + (0.30 * CASE 
                                    WHEN COUNT(DISTINCT CASE WHEN app.status = 'APPROVED' AND (e.event_date < CURRENT_DATE OR e.status = 'COMPLETED') THEN app.id END) > 0 THEN
                                        LEAST((COUNT(DISTINCT CASE WHEN att.attendance_status = 'PRESENT' THEN att.id END) * 100.0) / 
                                              COUNT(DISTINCT CASE WHEN app.status = 'APPROVED' AND (e.event_date < CURRENT_DATE OR e.status = 'COMPLETED') THEN app.id END), 100.0)
                                    ELSE 0.0 
                                  END)
                        + (0.30 * (AVG(CASE WHEN vr.id IS NOT NULL THEN (vr.punctuality + vr.teamwork + vr.effort) / 3.0 ELSE NULL END) / 5.0 * 100.0))
                    ELSE
                        ((4.0 / 7.0) * LEAST(COALESCE(SUM(CASE WHEN att.hours_status = 'APPROVED' THEN att.hours_worked ELSE 0 END), 0), 100.0))
                        + ((3.0 / 7.0) * CASE 
                                           WHEN COUNT(DISTINCT CASE WHEN app.status = 'APPROVED' AND (e.event_date < CURRENT_DATE OR e.status = 'COMPLETED') THEN app.id END) > 0 THEN
                                               LEAST((COUNT(DISTINCT CASE WHEN att.attendance_status = 'PRESENT' THEN att.id END) * 100.0) / 
                                                     COUNT(DISTINCT CASE WHEN app.status = 'APPROVED' AND (e.event_date < CURRENT_DATE OR e.status = 'COMPLETED') THEN app.id END), 100.0)
                                           ELSE 0.0 
                                         END)
                END
            , 2) AS score
        FROM volunteer_profiles vp
        JOIN users u ON vp.user_id = u.id
        LEFT JOIN applications app ON app.volunteer_id = u.id
        LEFT JOIN events e ON app.event_id = e.id
        LEFT JOIN attendance att ON att.application_id = app.id
        LEFT JOIN volunteer_ratings vr ON vr.attendance_id = att.id
        WHERE vp.show_on_leaderboard = TRUE 
          AND u.status = 'ACTIVE' 
          AND u.role = 'VOLUNTEER'
        GROUP BY vp.user_id, vp.full_name
        ORDER BY score DESC, verifiedHours DESC, vp.user_id ASC
        """,
        countQuery = """
        SELECT COUNT(vp.id)
        FROM volunteer_profiles vp
        JOIN users u ON vp.user_id = u.id
        WHERE vp.show_on_leaderboard = TRUE 
          AND u.status = 'ACTIVE' 
          AND u.role = 'VOLUNTEER'
        """,
        nativeQuery = true)
    Page<LeaderboardRowProjection> findLeaderboardPage(Pageable pageable);
}
