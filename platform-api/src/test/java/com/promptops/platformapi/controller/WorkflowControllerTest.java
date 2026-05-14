package com.promptops.platformapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptops.platformapi.config.WebMvcConfig;
import com.promptops.platformapi.dto.ExecuteCaseRequest;
import com.promptops.platformapi.dto.WorkflowRequest;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.exception.GlobalExceptionHandler;
import com.promptops.platformapi.interceptor.JwtAuthInterceptor;
import com.promptops.platformapi.service.WorkflowRunService;
import com.promptops.platformapi.util.JwtUtil;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({WorkflowController.class, GlobalExceptionHandler.class,
    WebMvcConfig.class, JwtAuthInterceptor.class})
class WorkflowControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private WorkflowRunService workflowRunService;

  @MockitoBean
  private JwtUtil jwtUtil;

  private static final String FAKE_TOKEN = "Bearer fake.jwt.token";

  @BeforeEach
  void setUp() {
    when(jwtUtil.getUserIdFromToken("fake.jwt.token")).thenReturn(1L);
    when(jwtUtil.getUsernameFromToken("fake.jwt.token")).thenReturn("testuser");
  }

  @Test
  void createWorkflow_success() throws Exception {
    Workflow workflow = new Workflow();
    workflow.setId(7L);
    workflow.setProjectId(1L);
    workflow.setName("RAG JSON");
    workflow.setTemplateId("rag_json");
    workflow.setConfigJson("{\"schema_id\":\"qa_answer\"}");

    when(workflowRunService.createWorkflow(eq(1L), eq("RAG JSON"), eq("rag_json"), any()))
        .thenReturn(workflow);

    WorkflowRequest request = new WorkflowRequest();
    request.setName("RAG JSON");
    request.setTemplateId("rag_json");
    request.setSchemaId("qa_answer");

    mockMvc.perform(post("/projects/1/workflows")
            .header("Authorization", FAKE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.templateId").value("rag_json"));
  }

  @Test
  void listWorkflows_success() throws Exception {
    Workflow workflow = new Workflow();
    workflow.setId(7L);
    workflow.setProjectId(1L);
    workflow.setName("RAG JSON");
    workflow.setTemplateId("rag_json");

    when(workflowRunService.findWorkflowsByProjectId(1L)).thenReturn(List.of(workflow));

    mockMvc.perform(get("/projects/1/workflows")
            .header("Authorization", FAKE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("RAG JSON"));
  }

  @Test
  void executeCase_success() throws Exception {
    Run run = new Run();
    run.setId(9L);
    run.setProjectId(1L);
    run.setWorkflowId(7L);
    run.setCaseId("case-1");
    run.setUserInput("What is the refund window?");
    run.setStatus("SUCCESS");
    run.setOutputJson("{\"answer\":\"30 days\"}");
    run.setCitationsJson("[\"Refund Policy\"]");
    run.setStartedAt(Instant.parse("2026-03-17T10:00:00Z"));
    run.setEndedAt(Instant.parse("2026-03-17T10:00:01Z"));

    when(workflowRunService.executeCase(eq(1L), any(ExecuteCaseRequest.class))).thenReturn(run);

    ExecuteCaseRequest request = new ExecuteCaseRequest();
    request.setWorkflowId(7L);
    request.setCaseId("case-1");
    request.setUserInput("What is the refund window?");
    request.setSchemaId("qa_answer");

    mockMvc.perform(post("/projects/1/execute-case")
            .header("Authorization", FAKE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.outputJson").value("{\"answer\":\"30 days\"}"));
  }

  @Test
  void getRun_success() throws Exception {
    Run run = new Run();
    run.setId(9L);
    run.setProjectId(1L);
    run.setWorkflowId(7L);
    run.setCaseId("case-1");
    run.setStatus("SUCCESS");

    when(workflowRunService.findRunById(9L)).thenReturn(run);

    mockMvc.perform(get("/runs/9")
            .header("Authorization", FAKE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.caseId").value("case-1"))
        .andExpect(jsonPath("$.status").value("SUCCESS"));
  }
}
