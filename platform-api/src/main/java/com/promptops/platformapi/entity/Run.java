package com.promptops.platformapi.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "runs")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "workflow_id", nullable = false)
  private Long workflowId;

  @Column(name = "case_id", nullable = false, length = 100)
  private String caseId;

  @Column(name = "user_input", columnDefinition = "TEXT")
  private String userInput;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "output_json", columnDefinition = "JSON")
  private String outputJson;

  @Column(name = "citations_json", columnDefinition = "JSON")
  private String citationsJson;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Transient
  private List<RunTrace> traces;
}
