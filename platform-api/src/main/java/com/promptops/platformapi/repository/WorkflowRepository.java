package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.Workflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

  List<Workflow> findByProjectId(Long projectId);

  Optional<Workflow> findByIdAndProjectId(Long id, Long projectId);
}
