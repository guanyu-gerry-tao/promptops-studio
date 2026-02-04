package com.promptops.platformapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.entity.User;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.ProjectRepository;
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
class ProjectServiceTest {

  @Autowired
  private UserService userService;

  @Autowired
  private ProjectService projectService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProjectRepository projectRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  // Test owner user, created in setUp
  private User owner;

  @BeforeEach
  void setUp() {
    // Delete order matters: projects reference users, so delete projects first
    projectRepository.deleteAll();
    userRepository.deleteAll();

    // Create a test user to act as project owner
    owner = new User();
    owner.setUsername("projectOwner");
    owner.setEmail("owner@test.com");
    owner.setPasswordHash(passwordEncoder.encode("password123"));
    owner.setDisplayName("Project Owner");
    userRepository.save(owner);
  }

  @Test
  void create() {
    Project project = projectService.create("My Project", "A test project", owner.getId());

    assertNotNull(project.getId());
    assertEquals("My Project", project.getName());
    assertEquals("A test project", project.getDescription());
    assertEquals(owner.getId(), project.getOwnerId());
    assertEquals("ACTIVE", project.getStatus());
    assertNotNull(project.getCreatedAt());
  }

  @Test
  void create_fail_ownerNotFound() {
    // Non-existent ownerId should throw exception
    assertThrows(BusinessException.class,
        () -> projectService.create("Bad Project", "desc", 99999L));
  }

  @Test
  void findById() {
    Project created = projectService.create("Find Me", "desc", owner.getId());
    Project found = projectService.findById(created.getId());

    assertEquals(created.getId(), found.getId());
    assertEquals("Find Me", found.getName());
  }

  @Test
  void findById_fail_notFound() {
    assertThrows(BusinessException.class, () -> projectService.findById(99999L));
  }

  @Test
  void findByOwnerId() {
    // Create 2 projects for the same owner
    projectService.create("Project A", "desc A", owner.getId());
    projectService.create("Project B", "desc B", owner.getId());

    List<Project> projects = projectService.findByOwnerId(owner.getId());
    assertEquals(2, projects.size());
  }

  @Test
  void findByOwnerId_empty() {
    // No projects created, should return empty list
    List<Project> projects = projectService.findByOwnerId(owner.getId());
    assertTrue(projects.isEmpty());
  }

  @Test
  void update() {
    Project created = projectService.create("Old Name", "Old desc", owner.getId());
    Project updated = projectService.update(created.getId(), "New Name", "New desc");

    assertEquals(created.getId(), updated.getId());
    assertEquals("New Name", updated.getName());
    assertEquals("New desc", updated.getDescription());
  }

  @Test
  void update_fail_notFound() {
    assertThrows(BusinessException.class,
        () -> projectService.update(99999L, "Name", "Desc"));
  }

  @Test
  void delete() {
    Project created = projectService.create("Delete Me", "desc", owner.getId());
    Long projectId = created.getId();

    projectService.delete(projectId);

    // Finding a deleted project should throw NOT_FOUND
    assertThrows(BusinessException.class, () -> projectService.findById(projectId));
  }

  @Test
  void delete_fail_notFound() {
    assertThrows(BusinessException.class, () -> projectService.delete(99999L));
  }
}
