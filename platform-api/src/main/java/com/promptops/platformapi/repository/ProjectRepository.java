package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ProjectRepository - Data access layer for Project entity.
 *
 * By extending JpaRepository, Spring Data JPA automatically provides:
 * - save(Project project) - save or update a project
 * - findById(Long id) - find a project by ID
 * - findAll() - find all projects
 * - delete(Project project) - delete a project
 * - deleteById(Long id) - delete a project by ID
 * - count() - count total projects
 * - existsById(Long id) - check if a project exists
 *
 * We only need to define additional custom query methods below.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

  /**
   * Find all projects owned by a specific user.
   *
   * Spring Data JPA auto-generates the query:
   * SELECT * FROM projects WHERE owner_id = ?
   *
   * @param ownerId the owner's user ID
   * @return list of projects owned by this user
   */
  List<Project> findByOwnerId(Long ownerId);

  /**
   * Find all projects with a specific status.
   *
   * Spring Data JPA auto-generates the query:
   * SELECT * FROM projects WHERE status = ?
   *
   * @param status the project status (ACTIVE, ARCHIVED, DELETED)
   * @return list of projects with this status
   */
  List<Project> findByStatus(String status);
}
