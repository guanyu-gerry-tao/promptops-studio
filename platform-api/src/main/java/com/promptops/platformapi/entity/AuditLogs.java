package com.promptops.platformapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Audit Logs entity class.
 *
 * Maps to the "audit_logs" table in the database.
 * Each audit log object represents one row in the table.
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLogs {

  /**
   * Log ID - primary key, auto-incremented.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * User ID who performed the action. Nullable for system operations.
   */
  @Column(name = "user_id")
  private Long userId;

  /**
   * Action performed on the resource.
   * Possible values: CREATE, UPDATE, DELETE, LOGIN, LOGOUT.
   */
  @Column
  private String action;

  /**
   * Resource type on which the action was performed.
   * Possible values: USER, PROJECT, KB, WORKFLOW, DATASET, RUN.
   */
  @Column(name = "resource_type", length = 50)
  private String resourceType;

  /**
   * Resource ID on which the action was performed.
   */
  @Column(name = "resource_id")
  private Long resourceId;

  /**
   * Details of the action performed.
   * Written in JSON format.
   */
  @Column
  private String details;
  //TODO: JSON conversion

  /**
   * IP address of the user who performed the action.
   */
  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  /**
   * User agent of the user who performed the action.
   */
  @Column(name = "user_agent", length = 500)
  private String userAgent;

  /**
   * Created timestamp - auto-generated on insert.
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
