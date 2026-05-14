package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExecuteCaseRequest {

  @NotNull(message = "Workflow ID is required")
  private Long workflowId;

  @NotBlank(message = "Case ID is required")
  private String caseId;

  @NotBlank(message = "User input is required")
  private String userInput;

  private String schemaId;
}
