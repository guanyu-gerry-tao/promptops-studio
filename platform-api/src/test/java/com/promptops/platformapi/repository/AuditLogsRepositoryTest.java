package com.promptops.platformapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promptops.platformapi.entity.AuditLogs;
import com.promptops.platformapi.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Tests for AuditLogsRepository.
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
public class AuditLogsRepositoryTest {

  @Autowired
  private AuditLogsRepository auditLogsRepository;

  @Autowired
  private UserRepository userRepository;

  /** Stores the ID of the first test user, used as userId for auditLogs1 and auditLogs2. */
  private Long user1Id;

  /** Stores the ID of the second test user, used as userId for auditLogs3. */
  private Long user2Id;

  @BeforeEach
  void setUp() {
    // Clean up existing data to ensure tests start from a known state.
    // Delete child table (audit_logs) first, then parent table (users).
    auditLogsRepository.deleteAll();
    userRepository.deleteAll();

    // Create real users first to satisfy the foreign key constraint on user_id.
    User user1 = new User();
    user1.setUsername("audituser1");
    user1.setEmail("audituser1@test.com");
    user1.setPasswordHash("hash1");
    user1.setDisplayName("Audit User One");
    user1Id = userRepository.save(user1).getId();

    User user2 = new User();
    user2.setUsername("audituser2");
    user2.setEmail("audituser2@test.com");
    user2.setPasswordHash("hash2");
    user2.setDisplayName("Audit User Two");
    user2Id = userRepository.save(user2).getId();

    AuditLogs auditLogs1 = new AuditLogs();
    auditLogs1.setUserId(user1Id);
    auditLogs1.setAction("CREATE");
    auditLogs1.setResourceType("PROJECT");
    auditLogs1.setResourceId(321L);
    auditLogs1.setDetails("Test Project Details");
    auditLogs1.setIpAddress("127.0.0.1");
    auditLogs1.setUserAgent("Test User Agent");

    AuditLogs auditLogs2 = new AuditLogs();
    auditLogs2.setUserId(user1Id);
    auditLogs2.setAction("UPDATE");
    auditLogs2.setResourceType("PROJECT");
    auditLogs2.setResourceId(321L);
    auditLogs2.setDetails("Updated Project Details");
    auditLogs2.setIpAddress("127.0.0.1");
    auditLogs2.setUserAgent("Test User Agent");

    AuditLogs auditLogs3 = new AuditLogs();
    auditLogs3.setUserId(user2Id);
    auditLogs3.setAction("CREATE");
    auditLogs3.setResourceType("USER");
    auditLogs3.setResourceId(user2Id);
    auditLogs3.setDetails("Test User Details");
    auditLogs3.setIpAddress("127.0.0.1");
    auditLogs3.setUserAgent("Test User Agent");

    auditLogsRepository.save(auditLogs1);
    auditLogsRepository.save(auditLogs2);
    auditLogsRepository.save(auditLogs3);
  }

  /**
   * Test: find all audit logs by userId.
   */
  @Test
  void testFindByUserId() {
    List<AuditLogs> result = auditLogsRepository.findByUserId(user1Id);

    assertEquals(2, result.size());
    assertEquals(user1Id, result.get(0).getUserId());
    assertEquals(result.get(0).getUserId(), result.get(1).getUserId());
    assertNotEquals(user2Id, result.get(0).getUserId());
  }

  /**
   * Test: not finding audit logs by userId should return empty list.
   */
  @Test
  void testFindByUserId_NotFound() {
    List<AuditLogs> result = auditLogsRepository.findByUserId(0L);
    assertTrue(result.isEmpty());
  }
}
