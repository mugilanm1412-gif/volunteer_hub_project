package com.volunteerhub.service;

import com.volunteerhub.entity.AuditLog;
import com.volunteerhub.entity.User;
import com.volunteerhub.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reusable service recording immutable administrative and governance action logs.
 */
@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records a new audit trail entry.
     *
     * @param actor user performing the action
     * @param action machine-readable action code (e.g. APPROVE_HOURS, REJECT_HOURS)
     * @param entityType domain entity type (e.g. ATTENDANCE, EVENT, NGO_PROFILE)
     * @param entityId target entity database identifier
     * @param details descriptive audit details
     * @return persisted AuditLog
     */
    public AuditLog logAction(User actor, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog(actor, action, entityType, entityId, details);
        return auditLogRepository.save(auditLog);
    }
}
