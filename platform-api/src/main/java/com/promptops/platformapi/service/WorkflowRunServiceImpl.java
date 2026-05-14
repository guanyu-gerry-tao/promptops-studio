package com.promptops.platformapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.dto.ExecuteCaseRequest;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunTrace;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.ProjectRepository;
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
public class WorkflowRunServiceImpl implements WorkflowRunService {

  private static final Logger log = LoggerFactory.getLogger(WorkflowRunServiceImpl.class);

  private final ProjectRepository projectRepository;
  private final WorkflowRepository workflowRepository;
  private final RunRepository runRepository;
  private final RunTraceRepository runTraceRepository;
  private final RestClient restClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WorkflowRunServiceImpl(
      ProjectRepository projectRepository,
      WorkflowRepository workflowRepository,
      RunRepository runRepository,
      RunTraceRepository runTraceRepository,
      @Value("${ai-runtime.base-url}") String aiRuntimeBaseUrl) {
    this.projectRepository = projectRepository;
    this.workflowRepository = workflowRepository;
    this.runRepository = runRepository;
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
  public Workflow createWorkflow(Long projectId, String name, String templateId, Map<String, Object> config) {
    requireProject(projectId);

    Workflow workflow = new Workflow();
    workflow.setProjectId(projectId);
    workflow.setName(name);
    workflow.setTemplateId(templateId);
    workflow.setConfigJson(toJson(config));
    workflow.setStatus("ACTIVE");
    return workflowRepository.save(workflow);
  }

  @Override
  public List<Workflow> findWorkflowsByProjectId(Long projectId) {
    requireProject(projectId);
    return workflowRepository.findByProjectId(projectId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Run executeCase(Long projectId, ExecuteCaseRequest request) {
    requireProject(projectId);
    Workflow workflow = workflowRepository.findByIdAndProjectId(request.getWorkflowId(), projectId)
        .orElseThrow(() -> new BusinessException(
            HttpStatus.NOT_FOUND, "Workflow not found with id: " + request.getWorkflowId()));

    Run run = new Run();
    run.setProjectId(projectId);
    run.setWorkflowId(workflow.getId());
    run.setCaseId(request.getCaseId());
    run.setUserInput(request.getUserInput());
    run.setStatus("RUNNING");
    run.setStartedAt(Instant.now());
    run = runRepository.save(run);

    try {
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("project_id", projectId);
      requestBody.put("workflow_id", workflow.getTemplateId());
      requestBody.put("case_id", request.getCaseId());
      requestBody.put("user_input", request.getUserInput());
      if (request.getSchemaId() != null && !request.getSchemaId().isBlank()) {
        requestBody.put("schema_id", request.getSchemaId());
      }

      Map<String, Object> response = restClient.post()
          .uri("/execute-case")
          .contentType(MediaType.APPLICATION_JSON)
          .body(requestBody)
          .retrieve()
          .body(Map.class);

      run.setStatus((String) response.getOrDefault("status", "FAILED"));
      run.setOutputJson(toJson(response.get("output_json")));
      run.setCitationsJson(toJson(response.get("citations")));
      run.setErrorMessage((String) response.get("error_message"));
      run.setEndedAt(Instant.now());
      run = runRepository.save(run);

      List<Map<String, Object>> traces = (List<Map<String, Object>>) response.getOrDefault("trace", List.of());
      for (Map<String, Object> trace : traces) {
        RunTrace runTrace = new RunTrace();
        runTrace.setRunId(run.getId());
        runTrace.setCaseId(request.getCaseId());
        runTrace.setNodeName((String) trace.get("node_name"));
        runTrace.setInputSummary((String) trace.get("input_summary"));
        runTrace.setOutputSummary((String) trace.get("output_summary"));
        runTrace.setLatencyMs(asInteger(trace.get("latency_ms")));
        runTrace.setTokenCount(asInteger(trace.get("token_count")));
        runTrace.setCitationsJson(toJson(trace.get("citations")));
        runTraceRepository.save(runTrace);
      }

      run.setTraces(runTraceRepository.findByRunIdOrderByIdAsc(run.getId()));
      log.info("Workflow run id={} completed with status={}", run.getId(), run.getStatus());
      return run;
    } catch (Exception e) {
      run.setStatus("FAILED");
      run.setErrorMessage("ai-runtime execute-case failed: " + e.getMessage());
      run.setEndedAt(Instant.now());
      log.error("Workflow run id={} failed: {}", run.getId(), e.getMessage());
      return runRepository.save(run);
    }
  }

  @Override
  public List<Run> findRunsByProjectId(Long projectId) {
    requireProject(projectId);
    return runRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  @Override
  public Run findRunById(Long runId) {
    Run run = runRepository.findById(runId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Run not found with id: " + runId));
    run.setTraces(runTraceRepository.findByRunIdOrderByIdAsc(runId));
    return run;
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
