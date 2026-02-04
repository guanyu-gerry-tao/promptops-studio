package com.promptops.platformapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO.
 *
 * JSON example: { "username": "gerry", "password": "123456" }
 */
@Data
public class LoginRequest {

    @NotBlank(message = "username can not be blank")
    private String username;

    @NotBlank(message = "password can not be blank")
    private String password;

}
