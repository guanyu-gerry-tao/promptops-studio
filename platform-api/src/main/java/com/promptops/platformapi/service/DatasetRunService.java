package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.RunRequest;
import com.promptops.platformapi.entity.Dataset;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import java.util.List;

public interface DatasetRunService {

  Dataset createDataset(Long projectId, String name, String content);

  List<Dataset> findDatasetsByProjectId(Long projectId);

  Run startRun(Long projectId, RunRequest request);

  List<RunCase> findCasesByRunId(Long runId);

  RunCase findCase(Long runId, String caseId);
}
