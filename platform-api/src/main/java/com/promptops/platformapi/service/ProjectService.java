package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.Project;
import java.util.List;

/**
 * Project service interface - defines business operations for project management.
 */
public interface ProjectService {

    /**
     * Create a new project.
     *
     * @param name        project name
     * @param description project description (optional)
     * @param ownerId     the user ID who owns this project
     * @return the newly created Project entity
     */
    Project create(String name, String description, Long ownerId);

    /**
     * Find a project by ID.
     *
     * @param id the project ID
     * @return the Project entity
     */
    Project findById(Long id);

    /**
     * Find all projects owned by a user.
     *
     * @param ownerId the owner's user ID
     * @return list of projects (empty list if none found)
     */
    List<Project> findByOwnerId(Long ownerId);

    /**
     * Update a project's name and description.
     *
     * @param id          the project ID to update
     * @param name        new project name
     * @param description new project description
     * @return the updated Project entity
     */
    Project update(Long id, String name, String description);

    /**
     * Delete a project by ID.
     *
     * @param id the project ID to delete
     */
    void delete(Long id);
}
