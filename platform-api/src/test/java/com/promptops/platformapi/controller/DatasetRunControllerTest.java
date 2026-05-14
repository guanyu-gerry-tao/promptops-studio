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
import com.promptops.platformapi.dto.DatasetRequest;
import com.promptops.platformapi.dto.RunRequest;
import com.promptops.platformapi.entity.Dataset;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.exception.GlobalExceptionHandler;
import com.promptops.platformapi.interceptor.JwtAuthInterceptor;
import com.promptops.platformapi.service.DatasetRunService;
import com.promptops.platformapi.util.JwtUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({DatasetRunController.class, GlobalExceptionHandler.class,
    WebMvcConfig.class, JwtAuthInterceptor.class})
class DatasetRunControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private DatasetRunService datasetRunService;

  @MockitoBean
  private JwtUtil jwtUtil;

  private static final String FAKE_TOKEN = "Bearer fake.jwt.token";

  @BeforeEach
  void setUp() {
    when(jwtUtil.getUserIdFromToken("fake.jwt.token")).thenReturn(1L);
    when(jwtUtil.getUsernameFromToken("fake.jwt.token")).thenReturn("testuser");
  }

  @Test
  void uploadDataset_success() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setId(3L);
    dataset.setProjectId(1L);
    dataset.setName("Refund QA");
    dataset.setItemsCount(2);

    when(datasetRunService.createDataset(eq(1L), eq("Refund QA"), any())).thenReturn(dataset);

    DatasetRequest request = new DatasetRequest();
    request.setName("Refund QA");
    request.setContent("{\"case_id\":\"case-1\",\"input\":\"Refund window?\"}\n{\"input\":\"Shipping?\"}");

    mockMvc.perform(post("/projects/1/datasets")
            .header("Authorization", FAKE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.itemsCount").value(2));
  }

  @Test
  void startRun_success() throws Exception {
    Run run = new Run();
    run.setId(8L);
    run.setProjectId(1L);
    run.setWorkflowId(7L);
    run.setDatasetId(3L);
    run.setStatus("SUCCESS");
    run.setTotalCases(2);
    run.setSuccessCases(2);
    run.setFailedCases(0);

    when(datasetRunService.startRun(eq(1L), any(RunRequest.class))).thenReturn(run);

    RunRequest request = new RunRequest();
    request.setWorkflowId(7L);
    request.setDatasetId(3L);
    request.setSchemaId("qa_answer");

    mockMvc.perform(post("/projects/1/runs")
            .header("Authorization", FAKE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.totalCases").value(2));
  }

  @Test
  void listRunCases_success() throws Exception {
    RunCase runCase = new RunCase();
    runCase.setId(11L);
    runCase.setRunId(8L);
    runCase.setCaseId("case-1");
    runCase.setStatus("SUCCESS");

    when(datasetRunService.findCasesByRunId(8L)).thenReturn(List.of(runCase));

    mockMvc.perform(get("/runs/8/cases")
            .header("Authorization", FAKE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].caseId").value("case-1"));
  }
}
