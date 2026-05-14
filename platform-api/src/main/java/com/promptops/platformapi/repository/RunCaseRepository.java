package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.RunCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunCaseRepository extends JpaRepository<RunCase, Long> {

  List<RunCase> findByRunIdOrderByIdAsc(Long runId);

  Optional<RunCase> findByRunIdAndCaseId(Long runId, String caseId);
}
