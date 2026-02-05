package com.promptops.platformapi.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Tests for ProjectRepository.
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
public class ProjectRepositoryTest {

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  /** Stores the ID of the first test user, used as ownerId for project1 and project2. */
  private Long owner1Id;

  /** Stores the ID of the second test user, used as ownerId for project3. */
  private Long owner2Id;

  @BeforeEach
  void setUp() {
    // Clean up existing data to ensure tests start from a known state.
    // Delete child table (projects) first, then parent table (users),
    // because of the foreign key constraint: projects.owner_id -> users.id.
    projectRepository.deleteAll();
    userRepository.deleteAll();

    // Create real users first to satisfy the foreign key constraint on owner_id.
    User owner1 = new User();
    owner1.setUsername("owner1");
    owner1.setEmail("owner1@test.com");
    owner1.setPasswordHash("hash1");
    owner1.setDisplayName("Owner One");
    owner1Id = userRepository.save(owner1).getId();

    User owner2 = new User();
    owner2.setUsername("owner2");
    owner2.setEmail("owner2@test.com");
    owner2.setPasswordHash("hash2");
    owner2.setDisplayName("Owner Two");
    owner2Id = userRepository.save(owner2).getId();

    Project project1 = new Project();
    project1.setName("Test Project1");
    project1.setDescription("Test Project Description");
    project1.setOwnerId(owner1Id);
    project1.setStatus("ACTIVE");

    Project project2 = new Project();
    project2.setName("Test Project2");
    project2.setDescription("Test Project Description");
    project2.setOwnerId(owner1Id);
    project2.setStatus("ARCHIVED");

    Project project3 = new Project();
    project3.setName("Test Project3");
    project3.setDescription("Test Project Description");
    project3.setOwnerId(owner2Id);
    project3.setStatus("DELETED");

    projectRepository.save(project1);
    projectRepository.save(project2);
    projectRepository.save(project3);
  }

  /**
   * Test: find project1 and project2 by ownerId.
   */
  @Test
  void testFindByOwnerId() {
    List<Project> result = projectRepository.findByOwnerId(owner1Id);

    assertEquals(2, result.size());
    assertEquals(owner1Id, result.get(0).getOwnerId());
    assertEquals(owner1Id, result.get(1).getOwnerId());
  }

  /**
   * Test: not finding any project by ownerId should return empty list.
   */
  @Test
  void testFindByOwnerId_NotFound() {
    List<Project> result = projectRepository.findByOwnerId(0L);

    assertTrue(result.isEmpty());
  }

  /**
   * Test: find projects by status.
   */
  @Test
  void testFindByStatus() {
    List<Project> result = projectRepository.findByStatus("ACTIVE");
    assertEquals(1, result.size());
    assertEquals("Test Project1", result.get(0).getName());
  }

  /**
   * Test: not finding projects by status should return empty list.
   */
  @Test
  void testFindByStatus_NotFound() {
    List<Project> result = projectRepository.findByStatus("WRONG STATUS");
    assertTrue(result.isEmpty());
  }
}
