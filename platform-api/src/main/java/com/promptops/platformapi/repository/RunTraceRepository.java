package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.RunTrace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunTraceRepository extends JpaRepository<RunTrace, Long> {

  List<RunTrace> findByRunIdOrderByIdAsc(Long runId);
}
