package com.volunteerhub.repository;

import com.volunteerhub.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Certificate entities.
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByAttendanceId(Long attendanceId);

    boolean existsByAttendanceId(Long attendanceId);

    /**
     * Retrieves all certificates earned by a volunteer with eager fetch joins to eliminate N+1 queries.
     */
    @Query("SELECT c FROM Certificate c " +
           "JOIN FETCH c.attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.volunteer v " +
           "JOIN FETCH app.event e " +
           "JOIN FETCH e.ngo ngo " +
           "WHERE v.id = :volunteerId " +
           "ORDER BY c.issuedOn DESC")
    List<Certificate> findByVolunteerIdWithDetails(@Param("volunteerId") Long volunteerId);

    /**
     * Retrieves a certificate with its complete associated graph for PDF rendering and ownership checks.
     */
    @Query("SELECT c FROM Certificate c " +
           "JOIN FETCH c.attendance a " +
           "JOIN FETCH a.application app " +
           "JOIN FETCH app.volunteer v " +
           "JOIN FETCH app.event e " +
           "JOIN FETCH e.ngo ngo " +
           "WHERE c.id = :id")
    Optional<Certificate> findByIdWithDetails(@Param("id") Long id);

    /**
     * Finds the latest certificate number matching the yearly prefix pattern (e.g. 'VH-2026-%')
     * to safely compute the next sequential number.
     */
    @Query(value = "SELECT certificate_number FROM certificates WHERE certificate_number LIKE :prefix ORDER BY certificate_number DESC, id DESC LIMIT 1",
           nativeQuery = true)
    Optional<String> findLatestCertificateNumberForPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(c) FROM Certificate c JOIN c.attendance a JOIN a.application app WHERE app.volunteer.id = :volunteerId")
    long countByVolunteerId(@Param("volunteerId") Long volunteerId);
}
