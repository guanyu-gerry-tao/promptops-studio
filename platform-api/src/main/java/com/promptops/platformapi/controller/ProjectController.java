package com.promptops.platformapi.controller;

import com.promptops.platformapi.dto.ProjectRequest;
import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Project CRUD controller. All paths require JWT authentication (handled by JwtAuthInterceptor).
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  /**
   * Create a new project for the current user.
   * POST /projects
   */
  @PostMapping
  public ResponseEntity<Project> create(@Valid @RequestBody ProjectRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    Project project = projectService.create(request.getName(), request.getDescription(), userId);
    return ResponseEntity.ok(project);
  }

  /**
   * Get all projects for the current user. GET /projects
   */
  @GetMapping
  public ResponseEntity<List<Project>> list(HttpServletRequest httpRequest) {
    Long ownerId = (Long) httpRequest.getAttribute("userId");
    List<Project> projects = projectService.findByOwnerId(ownerId);
    return ResponseEntity.ok(projects);
  }

  /**
   * Get a single project by ID. GET /projects/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<Project> get(@PathVariable Long id) {
    Project project = projectService.findById(id);
    return ResponseEntity.ok(project);
  }

  /**
   * Update a project. PUT /projects/{id}
   */
  @PutMapping("/{id}")
  public ResponseEntity<Project> update(
      @PathVariable Long id,
      @Valid @RequestBody ProjectRequest request) {
    Project project = projectService.update(id, request.getName(), request.getDescription());
    return ResponseEntity.ok(project);
  }

  /**
   * Delete a project. DELETE /projects/{id}
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    projectService.delete(id);
    return ResponseEntity.ok().build();
  }
}
