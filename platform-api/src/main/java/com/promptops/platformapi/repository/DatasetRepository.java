package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.Dataset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

  List<Dataset> findByProjectIdOrderByCreatedAtDesc(Long projectId);

  Optional<Dataset> findByIdAndProjectId(Long id, Long projectId);
}
