package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.AuditLogs;
import java.util.List;

/**
 * Audit logs service interface - defines business operations for audit logging.
 */
public interface AuditLogsService {

    /**
     * Record an audit log entry.
     *
     * @param userId       the user who performed the action (null for system actions)
     * @param action       the action performed (e.g., "CREATE", "UPDATE", "DELETE", "LOGIN")
     * @param resourceType the type of resource (e.g., "USER", "PROJECT")
     * @param resourceId   the ID of the resource being acted upon
     * @param details      additional details in JSON format (optional)
     * @param ipAddress    client IP address (optional)
     * @param userAgent    client user agent (optional)
     * @return the created AuditLogs entity
     */
    AuditLogs log(Long userId, String action, String resourceType, Long resourceId,
                  String details, String ipAddress, String userAgent);

    /**
     * Find all audit logs for a specific user.
     *
     * @param userId the user ID
     * @return list of audit logs (empty list if none found)
     */
    List<AuditLogs> findByUserId(Long userId);

    /**
     * Find all audit logs for a specific resource.
     *
     * @param resourceType the resource type (e.g., "PROJECT")
     * @param resourceId   the resource ID
     * @return list of audit logs (empty list if none found)
     */
    List<AuditLogs> findByResource(String resourceType, Long resourceId);
}
