package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for searching the knowledge base.
 * POST /projects/{projectId}/kb/search
 */
@Data
public class KbSearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    /**
     * How many results to return. Defaults to 5 if not specified.
     */
    private Integer topK;

    /**
     * Whether to generate an LLM answer based on search results.
     * Defaults to true if not specified.
     */
    private Boolean generateAnswer;
}
