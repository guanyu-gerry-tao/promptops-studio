package com.promptops.platformapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Project entity class.
 *
 * Maps to the "projects" table in the database.
 * Each Project object represents one row in the table.
 */
@Entity
@Table(name = "projects")
@Data
public class Project {

  /**
   * Project ID - primary key, auto-incremented.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Name of the project
   */
  @Column(nullable = false, length = 100)
  private String name;

  /**
   * Description of the project
   */
  @Column
  private String description;

  /**
   * the owner's ID of the project
   */
  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  /**
   * Status of the project, Status: ACTIVE, ARCHIVED, DELETED
   */
  @Column
  private String status;

  /**
   * Created timestamp - auto-generated on insert.
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Updated timestamp - auto-updated on modification.
   */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
