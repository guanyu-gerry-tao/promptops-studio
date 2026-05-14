package com.promptops.platformapi.controller;

import com.promptops.platformapi.dto.ExecuteCaseRequest;
import com.promptops.platformapi.dto.WorkflowRequest;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.service.WorkflowRunService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkflowController {

  private final WorkflowRunService workflowRunService;

  public WorkflowController(WorkflowRunService workflowRunService) {
    this.workflowRunService = workflowRunService;
  }

  @PostMapping("/projects/{projectId}/workflows")
  public ResponseEntity<Workflow> createWorkflow(
      @PathVariable Long projectId,
      @Valid @RequestBody WorkflowRequest request) {
    Map<String, Object> config = new HashMap<>();
    if (request.getSchemaId() != null && !request.getSchemaId().isBlank()) {
      config.put("schema_id", request.getSchemaId());
    }
    Workflow workflow = workflowRunService.createWorkflow(
        projectId, request.getName(), request.getTemplateId(), config);
    return ResponseEntity.ok(workflow);
  }

  @GetMapping("/projects/{projectId}/workflows")
  public ResponseEntity<List<Workflow>> listWorkflows(@PathVariable Long projectId) {
    return ResponseEntity.ok(workflowRunService.findWorkflowsByProjectId(projectId));
  }

  @PostMapping("/projects/{projectId}/execute-case")
  public ResponseEntity<Run> executeCase(
      @PathVariable Long projectId,
      @Valid @RequestBody ExecuteCaseRequest request) {
    return ResponseEntity.ok(workflowRunService.executeCase(projectId, request));
  }

  @GetMapping("/projects/{projectId}/runs")
  public ResponseEntity<List<Run>> listRuns(@PathVariable Long projectId) {
    return ResponseEntity.ok(workflowRunService.findRunsByProjectId(projectId));
  }

  @GetMapping("/runs/{runId}")
  public ResponseEntity<Run> getRun(@PathVariable Long runId) {
    return ResponseEntity.ok(workflowRunService.findRunById(runId));
  }
}
