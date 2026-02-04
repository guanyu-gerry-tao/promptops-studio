package com.promptops.platformapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promptops.platformapi.entity.AuditLogs;
import com.promptops.platformapi.entity.User;
import com.promptops.platformapi.repository.AuditLogsRepository;
import com.promptops.platformapi.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditLogsServiceTest {

  @Autowired
  private AuditLogsService auditLogsService;

  @Autowired
  private AuditLogsRepository auditLogsRepository;

  @Autowired
  private UserRepository userRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  // Test user, created in setUp
  private User testUser;

  @BeforeEach
  void setUp() {
    // Delete order matters: audit_logs reference users, so delete logs first
    auditLogsRepository.deleteAll();
    userRepository.deleteAll();

    // Create a test user
    testUser = new User();
    testUser.setUsername("auditTestUser");
    testUser.setEmail("audit@test.com");
    testUser.setPasswordHash(passwordEncoder.encode("password123"));
    testUser.setDisplayName("Audit Test User");
    userRepository.save(testUser);
  }

  @Test
  void createAuditLog() {
    AuditLogs log = auditLogsService.log(
        testUser.getId(),
        "CREATE",
        "PROJECT",
        1L,
        "{\"name\": \"New Project\"}",
        "127.0.0.1",
        "JUnit Test Agent"
    );

    assertNotNull(log.getId());
    assertEquals(testUser.getId(), log.getUserId());
    assertEquals("CREATE", log.getAction());
    assertEquals("PROJECT", log.getResourceType());
    assertEquals(1L, log.getResourceId());
    assertNotNull(log.getCreatedAt());
  }

  @Test
  void createAuditLog_systemAction() {
    // System actions can have null userId
    AuditLogs log = auditLogsService.log(
        null,
        "DELETE",
        "USER",
        99L,
        "{\"reason\": \"auto cleanup\"}",
        null,
        null
    );

    assertNotNull(log.getId());
    assertEquals(null, log.getUserId());
    assertEquals("DELETE", log.getAction());
  }

  @Test
  void findByUserId() {
    // Same user produces 2 log entries
    auditLogsService.log(testUser.getId(), "CREATE", "PROJECT", 1L, null, "127.0.0.1", "Agent");
    auditLogsService.log(testUser.getId(), "UPDATE", "PROJECT", 1L, null, "127.0.0.1", "Agent");

    List<AuditLogs> logs = auditLogsService.findByUserId(testUser.getId());
    assertEquals(2, logs.size());
  }

  @Test
  void findByUserId_empty() {
    // User has no logs, should return empty list
    List<AuditLogs> logs = auditLogsService.findByUserId(testUser.getId());
    assertTrue(logs.isEmpty());
  }

  @Test
  void findByResource() {
    // Log multiple actions on the same resource (PROJECT #42)
    auditLogsService.log(testUser.getId(), "CREATE", "PROJECT", 42L, null, null, null);
    auditLogsService.log(testUser.getId(), "UPDATE", "PROJECT", 42L, null, null, null);
    // This log targets a different resource, should NOT be returned
    auditLogsService.log(testUser.getId(), "CREATE", "USER", 1L, null, null, null);

    List<AuditLogs> logs = auditLogsService.findByResource("PROJECT", 42L);
    assertEquals(2, logs.size());
    // Verify all results match the queried resource
    for (AuditLogs log : logs) {
      assertEquals("PROJECT", log.getResourceType());
      assertEquals(42L, log.getResourceId());
    }
  }

  @Test
  void findByResource_empty() {
    List<AuditLogs> logs = auditLogsService.findByResource("WORKFLOW", 999L);
    assertTrue(logs.isEmpty());
  }
}
