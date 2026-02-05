package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.AuditLogs;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AuditLogsRepository - Data access layer for AuditLogs entity.
 *
 * By extending JpaRepository, Spring Data JPA automatically provides:
 * - save(AuditLogs log) - save an audit log entry
 * - findById(Long id) - find a log entry by ID
 * - findAll() - find all log entries
 * - delete(AuditLogs log) - delete a log entry
 * - deleteById(Long id) - delete a log entry by ID
 * - count() - count total log entries
 * - existsById(Long id) - check if a log entry exists
 *
 * We only need to define additional custom query methods below.
 */
@Repository
public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long> {

  /**
   * Find all audit logs for a specific user.
   *
   * Spring Data JPA auto-generates the query:
   * SELECT * FROM audit_logs WHERE user_id = ?
   *
   * @param userId the user ID to search for
   * @return list of audit log entries for this user
   */
  List<AuditLogs> findByUserId(Long userId);

  /**
   * Find all audit logs for a specific resource.
   *
   * Spring Data JPA auto-generates the query:
   * SELECT * FROM audit_logs WHERE resource_type = ? AND resource_id = ?
   *
   * @param resourceType the resource type (e.g., "PROJECT", "USER")
   * @param resourceId   the resource ID
   * @return list of audit log entries for this resource
   */
  List<AuditLogs> findByResourceTypeAndResourceId(String resourceType, Long resourceId);
}
