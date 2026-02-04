package com.promptops.platformapi.dto;

import lombok.Data;

/**
 * Login response DTO - returned after successful authentication.
 *
 * JSON example: { "token": "eyJ...", "userId": 1, "username": "gerry" }
 */
@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;

}
