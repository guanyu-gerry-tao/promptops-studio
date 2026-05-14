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

@Entity
@Table(name = "run_traces")
@Data
public class RunTrace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "case_id", nullable = false, length = 100)
  private String caseId;

  @Column(name = "node_name", nullable = false, length = 100)
  private String nodeName;

  @Column(name = "input_summary", columnDefinition = "TEXT")
  private String inputSummary;

  @Column(name = "output_summary", columnDefinition = "TEXT")
  private String outputSummary;

  @Column(name = "latency_ms")
  private Integer latencyMs;

  @Column(name = "token_count")
  private Integer tokenCount;

  @Column(name = "citations_json", columnDefinition = "JSON")
  private String citationsJson;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
