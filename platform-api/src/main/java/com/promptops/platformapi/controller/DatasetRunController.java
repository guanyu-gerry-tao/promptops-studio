package com.promptops.platformapi.controller;

import com.promptops.platformapi.dto.DatasetRequest;
import com.promptops.platformapi.dto.RunRequest;
import com.promptops.platformapi.entity.Dataset;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.service.DatasetRunService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatasetRunController {

  private final DatasetRunService datasetRunService;

  public DatasetRunController(DatasetRunService datasetRunService) {
    this.datasetRunService = datasetRunService;
  }

  @PostMapping("/projects/{projectId}/datasets")
  public ResponseEntity<Dataset> createDataset(
      @PathVariable Long projectId,
      @Valid @RequestBody DatasetRequest request) {
    return ResponseEntity.ok(datasetRunService.createDataset(projectId, request.getName(), request.getContent()));
  }

  @GetMapping("/projects/{projectId}/datasets")
  public ResponseEntity<List<Dataset>> listDatasets(@PathVariable Long projectId) {
    return ResponseEntity.ok(datasetRunService.findDatasetsByProjectId(projectId));
  }

  @PostMapping("/projects/{projectId}/runs")
  public ResponseEntity<Run> startRun(
      @PathVariable Long projectId,
      @Valid @RequestBody RunRequest request) {
    return ResponseEntity.ok(datasetRunService.startRun(projectId, request));
  }

  @GetMapping("/runs/{runId}/cases")
  public ResponseEntity<List<RunCase>> listCases(@PathVariable Long runId) {
    return ResponseEntity.ok(datasetRunService.findCasesByRunId(runId));
  }

  @GetMapping("/runs/{runId}/case/{caseId}")
  public ResponseEntity<RunCase> getCase(
      @PathVariable Long runId,
      @PathVariable String caseId) {
    return ResponseEntity.ok(datasetRunService.findCase(runId, caseId));
  }
}
