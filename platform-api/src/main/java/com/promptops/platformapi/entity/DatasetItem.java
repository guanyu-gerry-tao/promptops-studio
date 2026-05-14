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
@Table(name = "dataset_items")
@Data
public class DatasetItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "dataset_id", nullable = false)
  private Long datasetId;

  @Column(name = "case_id", nullable = false, length = 100)
  private String caseId;

  @Column(name = "input_text", nullable = false, columnDefinition = "TEXT")
  private String inputText;

  @Column(name = "tags_json", columnDefinition = "JSON")
  private String tagsJson;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
