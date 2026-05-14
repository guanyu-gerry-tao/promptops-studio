package com.promptops.platformapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "run_cases")
@Data
public class RunCase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "case_id", nullable = false, length = 100)
  private String caseId;

  @Column(name = "input_text", columnDefinition = "TEXT")
  private String inputText;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "output_json", columnDefinition = "JSON")
  private String outputJson;

  @Column(name = "citations_json", columnDefinition = "JSON")
  private String citationsJson;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
