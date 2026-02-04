package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Project create/update request DTO.
 *
 * JSON example: { "name": "My Project", "description": "A cool project" }
 */
@Data
public class ProjectRequest {

    @NotBlank(message = "name can not be blank")
    private String name;

    private String description;

}
