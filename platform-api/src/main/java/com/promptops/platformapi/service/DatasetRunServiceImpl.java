package com.promptops.platformapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.dto.RunRequest;
import com.promptops.platformapi.dto.RunRequestedEvent;
import com.promptops.platformapi.entity.Dataset;
import com.promptops.platformapi.entity.DatasetItem;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.entity.RunTrace;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.DatasetItemRepository;
import com.promptops.platformapi.repository.DatasetRepository;
import com.promptops.platformapi.repository.ProjectRepository;
import com.promptops.platformapi.repository.RunCaseRepository;
import com.promptops.platformapi.repository.RunRepository;
import com.promptops.platformapi.repository.RunTraceRepository;
import com.promptops.platformapi.repository.WorkflowRepository;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DatasetRunServiceImpl implements DatasetRunService {

  private static final Logger log = LoggerFactory.getLogger(DatasetRunServiceImpl.class);

  private final ProjectRepository projectRepository;
  private final DatasetRepository datasetRepository;
  private final DatasetItemRepository datasetItemRepository;
  private final WorkflowRepository workflowRepository;
  private final RunRepository runRepository;
  private final RunCaseRepository runCaseRepository;
  private final RunTraceRepository runTraceRepository;
  private final RestClient restClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DatasetRunServiceImpl(
      ProjectRepository projectRepository,
      DatasetRepository datasetRepository,
      DatasetItemRepository datasetItemRepository,
      WorkflowRepository workflowRepository,
      RunRepository runRepository,
      RunCaseRepository runCaseRepository,
      RunTraceRepository runTraceRepository,
      @Value("${ai-runtime.base-url}") String aiRuntimeBaseUrl) {
    this.projectRepository = projectRepository;
    this.datasetRepository = datasetRepository;
    this.datasetItemRepository = datasetItemRepository;
    this.workflowRepository = workflowRepository;
    this.runRepository = runRepository;
    this.runCaseRepository = runCaseRepository;
    this.runTraceRepository = runTraceRepository;

    HttpClient httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    this.restClient = RestClient.builder()
        .baseUrl(aiRuntimeBaseUrl)
        .requestFactory(new JdkClientHttpRequestFactory(httpClient))
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  @Override
  public Dataset createDataset(Long projectId, String name, String content) {
    requireProject(projectId);

    Dataset dataset = new Dataset();
    dataset.setProjectId(projectId);
    dataset.setName(name);
    dataset.setItemsCount(0);
    dataset.setStatus("READY");
    dataset = datasetRepository.save(dataset);

    int count = 0;
    for (String line : content.split("\\R")) {
      if (line.isBlank()) {
        continue;
      }
      count++;
      DatasetItem item = parseItem(dataset.getId(), count, line);
      datasetItemRepository.save(item);
    }

    if (count == 0) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "Dataset must contain at least one JSONL item");
    }

    dataset.setItemsCount(count);
    return datasetRepository.save(dataset);
  }

  @Override
  public List<Dataset> findDatasetsByProjectId(Long projectId) {
    requireProject(projectId);
    return datasetRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Run startRun(Long projectId, RunRequest request) {
    requireProject(projectId);
    Dataset dataset = datasetRepository.findByIdAndProjectId(request.getDatasetId(), projectId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Dataset not found with id: " + request.getDatasetId()));
    Workflow workflow = workflowRepository.findByIdAndProjectId(request.getWorkflowId(), projectId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Workflow not found with id: " + request.getWorkflowId()));
    List<DatasetItem> items = datasetItemRepository.findByDatasetIdOrderByIdAsc(dataset.getId());

    Run run = new Run();
    run.setProjectId(projectId);
    run.setWorkflowId(workflow.getId());
    run.setDatasetId(dataset.getId());
    run.setStatus("QUEUED");
    run.setTotalCases(items.size());
    run.setSuccessCases(0);
    run.setFailedCases(0);
    run = runRepository.save(run);

    RunRequestedEvent event = new RunRequestedEvent(
        run.getId(), projectId, workflow.getId(), dataset.getId(), request.getSchemaId());
    log.info("run.requested payload={}", toJson(event));

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
        requestBody.put("project_id", projectId);
        requestBody.put("workflow_id", workflow.getTemplateId());
        requestBody.put("case_id", item.getCaseId());
        requestBody.put("user_input", item.getInputText());
        if (request.getSchemaId() != null && !request.getSchemaId().isBlank()) {
          requestBody.put("schema_id", request.getSchemaId());
        }

        Map<String, Object> response = restClient.post()
            .uri("/execute-case")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(Map.class);

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

  @Override
  public List<RunCase> findCasesByRunId(Long runId) {
    return runCaseRepository.findByRunIdOrderByIdAsc(runId);
  }

  @Override
  public RunCase findCase(Long runId, String caseId) {
    return runCaseRepository.findByRunIdAndCaseId(runId, caseId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Run case not found: " + caseId));
  }

  private DatasetItem parseItem(Long datasetId, int lineNumber, String line) {
    try {
      JsonNode node = objectMapper.readTree(line);
      String caseId = node.hasNonNull("case_id") ? node.get("case_id").asText() : "case-" + lineNumber;
      String input = node.hasNonNull("input") ? node.get("input").asText() : node.path("user_input").asText();
      if (input == null || input.isBlank()) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Dataset line " + lineNumber + " missing input");
      }
      DatasetItem item = new DatasetItem();
      item.setDatasetId(datasetId);
      item.setCaseId(caseId);
      item.setInputText(input);
      item.setTagsJson(toJson(node.get("tags")));
      return item;
    } catch (JsonProcessingException e) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid JSONL at line " + lineNumber + ": " + e.getMessage());
    }
  }

  private void requireProject(Long projectId) {
    projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Project not found with id: " + projectId));
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
