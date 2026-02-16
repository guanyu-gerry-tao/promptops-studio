package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for uploading a document to the knowledge base.
 * POST /projects/{projectId}/kb/docs
 */
@Data
public class KbDocRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;
}
