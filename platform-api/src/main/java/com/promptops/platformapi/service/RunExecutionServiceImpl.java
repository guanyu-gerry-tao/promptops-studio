package com.promptops.platformapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.dto.RunRequestedEvent;
import com.promptops.platformapi.entity.DatasetItem;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.entity.RunTrace;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.DatasetItemRepository;
import com.promptops.platformapi.repository.RunCaseRepository;
import com.promptops.platformapi.repository.RunRepository;
import com.promptops.platformapi.repository.RunTraceRepository;
import com.promptops.platformapi.repository.WorkflowRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RunExecutionServiceImpl implements RunExecutionService {

  private final DatasetItemRepository datasetItemRepository;
  private final WorkflowRepository workflowRepository;
  private final RunRepository runRepository;
  private final RunCaseRepository runCaseRepository;
  private final RunTraceRepository runTraceRepository;
  private final AiRuntimeClient aiRuntimeClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RunExecutionServiceImpl(
      DatasetItemRepository datasetItemRepository,
      WorkflowRepository workflowRepository,
      RunRepository runRepository,
      RunCaseRepository runCaseRepository,
      RunTraceRepository runTraceRepository,
      AiRuntimeClient aiRuntimeClient) {
    this.datasetItemRepository = datasetItemRepository;
    this.workflowRepository = workflowRepository;
    this.runRepository = runRepository;
    this.runCaseRepository = runCaseRepository;
    this.runTraceRepository = runTraceRepository;
    this.aiRuntimeClient = aiRuntimeClient;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Run execute(RunRequestedEvent event) {
    Run run = runRepository.findById(event.getRunId())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Run not found with id: " + event.getRunId()));

    if ("SUCCESS".equals(run.getStatus()) || "FAILED".equals(run.getStatus())) {
      return run;
    }

    Workflow workflow = workflowRepository.findById(event.getWorkflowId())
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Workflow not found with id: " + event.getWorkflowId()));
    List<DatasetItem> items = datasetItemRepository.findByDatasetIdOrderByIdAsc(event.getDatasetId());

    run.setStatus("RUNNING");
    run.setStartedAt(Instant.now());
    run = runRepository.save(run);

    int success = 0;
    int failed = 0;
    for (DatasetItem item : items) {
      RunCase runCase = new RunCase();
      runCase.setRunId(run.getId());
      runCase.setCaseId(item.getCaseId());
      runCase.setInputText(item.getInputText());
      runCase.setStatus("RUNNING");
      runCase = runCaseRepository.save(runCase);

      try {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("project_id", event.getProjectId());
        requestBody.put("workflow_id", workflow.getTemplateId());
        requestBody.put("case_id", item.getCaseId());
        requestBody.put("user_input", item.getInputText());
        if (event.getSchemaId() != null && !event.getSchemaId().isBlank()) {
          requestBody.put("schema_id", event.getSchemaId());
        }

        Map<String, Object> response = aiRuntimeClient.executeCase(requestBody);

        String status = (String) response.getOrDefault("status", "FAILED");
        runCase.setStatus(status);
        runCase.setOutputJson(toJson(response.get("output_json")));
        runCase.setCitationsJson(toJson(response.get("citations")));
        runCase.setErrorMessage((String) response.get("error_message"));
        runCaseRepository.save(runCase);

        List<Map<String, Object>> traces = (List<Map<String, Object>>) response.getOrDefault("trace", List.of());
        for (Map<String, Object> trace : traces) {
          RunTrace runTrace = new RunTrace();
          runTrace.setRunId(run.getId());
          runTrace.setCaseId(item.getCaseId());
          runTrace.setNodeName((String) trace.get("node_name"));
          runTrace.setInputSummary((String) trace.get("input_summary"));
          runTrace.setOutputSummary((String) trace.get("output_summary"));
          runTrace.setLatencyMs(asInteger(trace.get("latency_ms")));
          runTrace.setTokenCount(asInteger(trace.get("token_count")));
          runTrace.setCitationsJson(toJson(trace.get("citations")));
          runTraceRepository.save(runTrace);
        }

        if ("SUCCESS".equals(status)) {
          success++;
        } else {
          failed++;
        }
      } catch (Exception e) {
        failed++;
        runCase.setStatus("FAILED");
        runCase.setErrorMessage("ai-runtime execute-case failed: " + e.getMessage());
        runCaseRepository.save(runCase);
      }
    }

    run.setSuccessCases(success);
    run.setFailedCases(failed);
    run.setStatus(failed == 0 ? "SUCCESS" : "FAILED");
    run.setEndedAt(Instant.now());
    return runRepository.save(run);
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON: " + e.getMessage());
    }
  }

  private Integer asInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Integer integer) {
      return integer;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.valueOf(value.toString());
  }
}
