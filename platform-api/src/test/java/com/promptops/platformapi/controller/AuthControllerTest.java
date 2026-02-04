package com.promptops.platformapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.dto.LoginRequest;
import com.promptops.platformapi.dto.RegisterRequest;
import com.promptops.platformapi.entity.User;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.exception.GlobalExceptionHandler;
import com.promptops.platformapi.service.UserService;
import com.promptops.platformapi.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AuthController tests using MockMvc.
 *
 * @WebMvcTest only loads the Controller layer (not Service, not Repository). We use @MockitoBean to
 * provide fake Service/JwtUtil implementations.
 */
@WebMvcTest({AuthController.class, GlobalExceptionHandler.class})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtUtil jwtUtil;

  @Test
  void register_success() throws Exception {
    // Arrange: set up mock behavior
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setUsername("gerry");
    mockUser.setEmail("gerry@test.com");
    mockUser.setDisplayName("Gerry");

    when(userService.register(
        "gerry",
        "gerry@test.com",
        "password123",
        "Gerry")
    )
        .thenReturn(mockUser);

    // Act & Assert: send POST request and verify response
    RegisterRequest request = new RegisterRequest();
    request.setUsername("gerry");
    request.setEmail("gerry@test.com");
    request.setPassword("password123");
    request.setDisplayName("Gerry");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("gerry"))
        .andExpect(jsonPath("$.email").value("gerry@test.com"));
  }

  @Test
  void register_fail_blankUsername() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("");  // blank!
    request.setEmail("test@test.com");
    request.setPassword("password123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_fail_invalidEmail() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("gerry");
    request.setEmail("not-an-email");  // invalid format!
    request.setPassword("password123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_fail_duplicateUsername() throws Exception {
    when(userService.register(anyString(), anyString(), anyString(), any()))
        .thenThrow(new BusinessException(HttpStatus.CONFLICT, "Username already taken"));

    RegisterRequest request = new RegisterRequest();
    request.setUsername("existing");
    request.setEmail("new@test.com");
    request.setPassword("password123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Username already taken"));
  }

  @Test
  void login_success() throws Exception {
    String fakeToken = "fake.jwt.token";
    when(userService.login("gerry", "password123")).thenReturn(fakeToken);
    when(jwtUtil.getUserIdFromToken(fakeToken)).thenReturn(1L);
    when(jwtUtil.getUsernameFromToken(fakeToken)).thenReturn("gerry");

    LoginRequest request = new LoginRequest();
    request.setUsername("gerry");
    request.setPassword("password123");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(fakeToken))
        .andExpect(jsonPath("$.userId").value(1))
        .andExpect(jsonPath("$.username").value("gerry"));
  }

  @Test
  void login_fail_wrongPassword() throws Exception {
    when(userService.login("gerry", "wrong"))
        .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

    LoginRequest request = new LoginRequest();
    request.setUsername("gerry");
    request.setPassword("wrong");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }
}
