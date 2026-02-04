package com.promptops.platformapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Register request DTO.
 *
 * JSON example:
 * { "username": "gerry", "email": "gerry@example.com", "password": "123456", "displayName": "Gerry" }
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "username can not be blank")
    private String username;

    @NotBlank(message = "email can not be blank")
    @Email(message = "email format is invalid")
    private String email;

    @NotBlank(message = "password can not be blank")
    private String password;

    private String displayName;

}
