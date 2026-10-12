package com.volunteerhub.repository;

import com.volunteerhub.entity.Application;
import com.volunteerhub.entity.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Application (registration) entities.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Application a WHERE a.event.id = :eventId AND a.status = :status")
    List<Application> findApprovedWithLock(@Param("eventId") Long eventId, @Param("status") ApplicationStatus status);

    boolean existsByVolunteerIdAndEventId(Long volunteerId, Long eventId);

    Optional<Application> findByVolunteerIdAndEventId(Long volunteerId, Long eventId);

    long countByEventIdAndStatus(Long eventId, ApplicationStatus status);

    long countByEventId(Long eventId);

    List<Application> findByEventIdOrderByAppliedAtAsc(Long eventId);

    List<Application> findByEventIdAndStatus(Long eventId, ApplicationStatus status);

    @Query("SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n " +
           "WHERE a.volunteer.id = :volunteerId AND a.status = 'APPROVED' " +
           "AND e.eventDate >= :today AND e.status != 'COMPLETED' AND e.status != 'CANCELLED'")
    Page<Application> findUpcomingApplications(@Param("volunteerId") Long volunteerId,
                                              @Param("today") LocalDate today,
                                              Pageable pageable);

    @Query("SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n " +
           "WHERE a.volunteer.id = :volunteerId AND a.status = 'APPROVED' " +
           "AND (e.eventDate < :today OR e.status = 'COMPLETED')")
    Page<Application> findCompletedApplications(@Param("volunteerId") Long volunteerId,
                                               @Param("today") LocalDate today,
                                               Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a " +
           "WHERE a.volunteer.id = :volunteerId AND a.status = 'APPROVED' " +
           "AND (a.event.eventDate < :today OR a.event.status = 'COMPLETED')")
    int countCompletedApplications(@Param("volunteerId") Long volunteerId,
                                   @Param("today") LocalDate today);

    @Query("SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n " +
           "WHERE a.volunteer.id = :volunteerId AND a.status = 'PENDING'")
    Page<Application> findPendingApplications(@Param("volunteerId") Long volunteerId,
                                             Pageable pageable);

    @Query("SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n " +
           "WHERE a.volunteer.id = :volunteerId AND (a.status = 'CANCELLED' OR a.status = 'REJECTED' OR e.status = 'CANCELLED')")
    Page<Application> findCancelledOrRejectedApplications(@Param("volunteerId") Long volunteerId,
                                                         Pageable pageable);

    @Query("SELECT a FROM Application a JOIN FETCH a.event e JOIN FETCH e.ngo n " +
           "WHERE a.volunteer.id = :volunteerId")
    Page<Application> findAllVolunteerApplications(@Param("volunteerId") Long volunteerId,
                                                  Pageable pageable);

    long countByVolunteerIdAndStatus(Long volunteerId, ApplicationStatus status);

    long countByEventNgoId(Long ngoId);

    long countByEventNgoIdAndStatus(Long ngoId, ApplicationStatus status);

    @Query("SELECT a FROM Application a " +
           "JOIN FETCH a.volunteer v " +
           "JOIN FETCH a.event e " +
           "WHERE a.status = 'APPROVED' " +
           "AND e.status = 'PUBLISHED' " +
           "AND e.eventDate = :eventDate")
    List<Application> findApprovedApplicationsForEventDate(@Param("eventDate") LocalDate eventDate);
}
