package com.volunteerhub.repository;

import com.volunteerhub.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for platform audit logs.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    java.util.List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
           @org.springframework.data.repository.query.Param("action") String action,
           @org.springframework.data.repository.query.Param("entityType") String entityType,
           Pageable pageable);
}
