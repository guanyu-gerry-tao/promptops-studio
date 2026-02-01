package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserRepository.
 *
 * @DataJpaTest does the following:
 * 1. Only loads JPA-related configuration (faster than full Spring context)
 * 2. Auto-rolls back after each test method (no dirty data left behind)
 *
 * @AutoConfigureTestDatabase(replace = NONE) tells Spring to use real MySQL
 * instead of replacing with H2 in-memory database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private AuditLogsRepository auditLogsRepository;

  @BeforeEach
  void setUp() {
    // Clean up existing data to ensure tests start from a known state.
    // Delete child tables first (projects, audit_logs), then parent table (users),
    // because of foreign key constraints referencing users.id.
    projectRepository.deleteAll();
    auditLogsRepository.deleteAll();
    userRepository.deleteAll();

    User user = new User();
    user.setUsername("john");
    user.setEmail("john@example.com");
    user.setPasswordHash("hashed_password_123");
    user.setDisplayName("John Doe");

    userRepository.save(user);
  }

  /**
   * Test: find by username.
   */
  @Test
  void testFindByUsername() {

    Optional<User> result = userRepository.findByUsername("john");

    assertTrue(result.isPresent(), "should find the user");
    assertEquals("john", result.get().getUsername(), "username should match");
    assertEquals("john@example.com", result.get().getEmail(), "email should match");
  }

  /**
   * Test: find a user by email.
   */
  @Test
  void testFindByEmail() {
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
