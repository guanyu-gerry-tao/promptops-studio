package com.promptops.platformapi.controller;

import com.promptops.platformapi.dto.LoginRequest;
import com.promptops.platformapi.dto.LoginResponse;
import com.promptops.platformapi.dto.RegisterRequest;
import com.promptops.platformapi.entity.User;
import com.promptops.platformapi.service.UserService;
import com.promptops.platformapi.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller - handles user registration and login. All paths under /auth are public
 * (excluded from JWT interceptor).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserService userService;
  private final JwtUtil jwtUtil;

  public AuthController(UserService userService, JwtUtil jwtUtil) {
    this.userService = userService;
    this.jwtUtil = jwtUtil;
  }

  /**
   * Register a new user. POST /auth/register
   */
  @PostMapping("/register")
  public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
    User user = userService.register(
        request.getUsername(),
        request.getEmail(),
        request.getPassword(),
        request.getDisplayName()
    );
    return ResponseEntity.ok(user);
  }

  /**
   * Login and return a JWT token with user info.
   * POST /auth/login
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    String token = userService.login(
        request.getUsername(),
        request.getPassword()
    );

    Long userId = jwtUtil.getUserIdFromToken(token);
    String username = jwtUtil.getUsernameFromToken(token);

    LoginResponse loginResponse = new LoginResponse();
    loginResponse.setToken(token);
    loginResponse.setUserId(userId);
    loginResponse.setUsername(username);
    return ResponseEntity.ok(loginResponse);
  }

}
