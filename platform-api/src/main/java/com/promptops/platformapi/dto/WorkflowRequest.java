package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowRequest {

  @NotBlank(message = "Workflow name is required")
  private String name;

  @NotBlank(message = "Template ID is required")
  private String templateId;

  private String schemaId;
}
