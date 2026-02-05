package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.ProjectRepository;
import com.promptops.platformapi.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ProjectService}.
 *
 * Handles CRUD business logic for projects.
 * Follows the same "Interface + Impl" pattern as UserServiceImpl.
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // Constructor Injection: Spring auto-wires the repositories
    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Project create(String name, String description, Long ownerId) {
        // Verify the owner exists before creating the project
        userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Owner not found with id: " + ownerId));

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setOwnerId(ownerId);
        project.setStatus("ACTIVE");
        return projectRepository.save(project);
    }

    @Override
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Project not found with id: " + id));
    }

    @Override
    public List<Project> findByOwnerId(Long ownerId) {
        return projectRepository.findByOwnerId(ownerId);
    }

    @Override
    public Project update(Long id, String name, String description) {
        // Reuse findById() which throws NOT_FOUND if absent
        Project project = findById(id);
        project.setName(name);
        project.setDescription(description);
        // save() does INSERT when id is null, UPDATE when id exists
        return projectRepository.save(project);
    }

    @Override
    public void delete(Long id) {
        // Ensure project exists; throws NOT_FOUND otherwise
        Project project = findById(id);
        projectRepository.delete(project);
    }
}
