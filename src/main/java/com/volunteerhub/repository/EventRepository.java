package com.volunteerhub.repository;

import com.volunteerhub.entity.Event;
import com.volunteerhub.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Data JPA repository for Event entities.
 * Includes pessimistic locking query to guarantee atomic capacity checks and
 * prevent overbooking.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Obtains an exclusive PESSIMISTIC_WRITE lock on the event row during
     * registration and approval.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    /**
     * Lists events created by a specific NGO organization.
     */
    Page<Event> findByNgoId(Long ngoId, Pageable pageable);

    /**
     * Comprehensive query for searching and filtering published volunteering
     * events.
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status " +
            "AND (:category IS NULL OR :category = '' OR e.category = :category) " +
            "AND (:location IS NULL OR :location = '' OR LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "AND (:from IS NULL OR e.eventDate >= :from) " +
            "AND (:to IS NULL OR e.eventDate <= :to) " +
            "AND (:q IS NULL OR :q = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:hasSlots IS NULL OR :hasSlots = false OR e.capacity > (SELECT COUNT(a) FROM Application a WHERE a.event = e AND a.status = 'APPROVED'))")
    Page<Event> searchEvents(
            @Param("status") EventStatus status,
            @Param("category") String category,
            @Param("location") String location,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            @Param("hasSlots") Boolean hasSlots,
            Pageable pageable);
}
