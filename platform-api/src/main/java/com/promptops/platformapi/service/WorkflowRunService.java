package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.ExecuteCaseRequest;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.Workflow;
import java.util.List;
import java.util.Map;

public interface WorkflowRunService {

  Workflow createWorkflow(Long projectId, String name, String templateId, Map<String, Object> config);

  List<Workflow> findWorkflowsByProjectId(Long projectId);

  Run executeCase(Long projectId, ExecuteCaseRequest request);

  List<Run> findRunsByProjectId(Long projectId);

  Run findRunById(Long runId);
}
