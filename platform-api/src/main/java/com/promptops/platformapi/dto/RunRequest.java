package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunRequest {

  @NotNull(message = "Workflow ID is required")
  private Long workflowId;

  @NotNull(message = "Dataset ID is required")
  private Long datasetId;

  private String schemaId;
}
