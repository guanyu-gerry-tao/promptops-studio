package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasetRequest {

  @NotBlank(message = "Dataset name is required")
  private String name;

  @NotBlank(message = "Dataset content is required")
  private String content;
}
