package com.promptops.platformapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RunRequestedEvent {

  private Long runId;
  private Long projectId;
  private Long workflowId;
  private Long datasetId;
  private String schemaId;
}
