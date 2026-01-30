package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserRepository.
 *
 * @DataJpaTest does the following:
 * 1. Only loads JPA-related configuration (faster than full Spring context)
 * 2. Uses H2 in-memory database (does not affect real MySQL)
 * 3. Auto-rolls back after each test method (no dirty data left behind)
 */
@DataJpaTest
public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    User user = new User();
    user.setUsername("john");
    user.setEmail("john@example.com");
    user.setPasswordHash("hashed_password_123");
    user.setDisplayName("John Doe");

    userRepository.save(user);
  }

  /**
   * Test: save a user and find by username.
   */
  @Test
  void testSaveAndFindByUsername() {

    Optional<User> result = userRepository.findByUsername("john");

    assertTrue(result.isPresent(), "should find the user");
    assertEquals("john", result.get().getUsername(), "username should match");
    assertEquals("john@example.com", result.get().getEmail(), "email should match");
  }

  /**
   * Test: find a user by email.
   */
  @Test
  void testSaveAndFindByEmail() {
    Optional<User> result = userRepository.findByEmail("john@example.com");

    assertEquals("john", result.get().getUsername());
    assertEquals("john@example.com", result.get().getEmail());
  }

  /**
   * Test: finding a non-existent username should return empty.
   */
  @Test
  void testFindByUsername_NotFound() {
    Optional<User> result = userRepository.findByUsername("nonexistent");

    assertFalse(result.isPresent(), "should not find the user");
    assertTrue(result.isEmpty(), "result should be empty");
  }

  /**
   * Test: finding a non-existent email should return empty.
   */
  @Test
  void testFindByEmail_NotFound() {
    Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

    assertTrue(result.isEmpty(), "result should be empty");
  }
}
