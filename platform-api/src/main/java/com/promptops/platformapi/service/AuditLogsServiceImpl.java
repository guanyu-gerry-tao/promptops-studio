package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.AuditLogs;
import com.promptops.platformapi.repository.AuditLogsRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link AuditLogsService}.
 *
 * Records and queries audit log entries.
 * Tracks who did what to which resource and when.
 */
@Service
public class AuditLogsServiceImpl implements AuditLogsService {

    private final AuditLogsRepository auditLogsRepository;

    public AuditLogsServiceImpl(AuditLogsRepository auditLogsRepository) {
        this.auditLogsRepository = auditLogsRepository;
    }

    @Override
    public AuditLogs log(Long userId, String action, String resourceType, Long resourceId,
                         String details, String ipAddress, String userAgent) {
        AuditLogs auditLog = new AuditLogs();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        // createdAt is auto-populated by @CreationTimestamp
        return auditLogsRepository.save(auditLog);
    }

    @Override
    public List<AuditLogs> findByUserId(Long userId) {
        return auditLogsRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLogs> findByResource(String resourceType, Long resourceId) {
        return auditLogsRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }
}
