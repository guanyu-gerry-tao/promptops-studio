package com.promptops.platformapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.promptops.platformapi.dto.RunRequestedEvent;
import com.promptops.platformapi.entity.DatasetItem;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.repository.DatasetItemRepository;
import com.promptops.platformapi.repository.RunCaseRepository;
import com.promptops.platformapi.repository.RunRepository;
import com.promptops.platformapi.repository.RunTraceRepository;
import com.promptops.platformapi.repository.WorkflowRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunExecutionServiceImplTest {

  @Mock
  private DatasetItemRepository datasetItemRepository;
  @Mock
  private WorkflowRepository workflowRepository;
  @Mock
  private RunRepository runRepository;
  @Mock
  private RunCaseRepository runCaseRepository;
  @Mock
  private RunTraceRepository runTraceRepository;
  @Mock
  private AiRuntimeClient aiRuntimeClient;

  @Test
  void execute_runsDatasetItemsAndUpdatesRunCounters() {
    Run run = new Run();
    run.setId(8L);
    run.setProjectId(1L);
    run.setWorkflowId(7L);
    run.setDatasetId(3L);
    run.setStatus("QUEUED");

    Workflow workflow = new Workflow();
    workflow.setId(7L);
    workflow.setTemplateId("rag_json");

    DatasetItem item = new DatasetItem();
    item.setId(5L);
    item.setDatasetId(3L);
    item.setCaseId("case-1");
    item.setInputText("What is the refund window?");

    when(runRepository.findById(8L)).thenReturn(Optional.of(run));
    when(workflowRepository.findById(7L)).thenReturn(Optional.of(workflow));
    when(datasetItemRepository.findByDatasetIdOrderByIdAsc(3L)).thenReturn(List.of(item));
    when(runCaseRepository.save(any(RunCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(runRepository.save(any(Run.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(aiRuntimeClient.executeCase(any())).thenReturn(Map.of(
        "status", "SUCCESS",
        "output_json", Map.of("answer", "30 days"),
        "citations", List.of("Refund Policy"),
        "trace", List.of()));

    RunExecutionServiceImpl service = new RunExecutionServiceImpl(
        datasetItemRepository,
        workflowRepository,
        runRepository,
        runCaseRepository,
        runTraceRepository,
        aiRuntimeClient);

    Run result = service.execute(new RunRequestedEvent(8L, 1L, 7L, 3L, "qa_answer"));

    assertThat(result.getStatus()).isEqualTo("SUCCESS");
    assertThat(result.getSuccessCases()).isEqualTo(1);
    assertThat(result.getFailedCases()).isZero();

    ArgumentCaptor<RunCase> caseCaptor = ArgumentCaptor.forClass(RunCase.class);
    org.mockito.Mockito.verify(runCaseRepository, org.mockito.Mockito.times(2)).save(caseCaptor.capture());
    assertThat(caseCaptor.getAllValues().get(1).getStatus()).isEqualTo("SUCCESS");
    assertThat(caseCaptor.getAllValues().get(1).getOutputJson()).contains("30 days");
  }
}
