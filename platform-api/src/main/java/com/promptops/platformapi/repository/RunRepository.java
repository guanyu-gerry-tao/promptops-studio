package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.Run;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunRepository extends JpaRepository<Run, Long> {

  List<Run> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
