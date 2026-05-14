package com.promptops.platformapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promptops.platformapi.dto.RunRequest;
import com.promptops.platformapi.dto.RunRequestedEvent;
import com.promptops.platformapi.entity.Dataset;
import com.promptops.platformapi.entity.DatasetItem;
import com.promptops.platformapi.entity.Project;
import com.promptops.platformapi.entity.Run;
import com.promptops.platformapi.entity.RunCase;
import com.promptops.platformapi.entity.Workflow;
import com.promptops.platformapi.repository.DatasetItemRepository;
import com.promptops.platformapi.repository.DatasetRepository;
import com.promptops.platformapi.repository.ProjectRepository;
import com.promptops.platformapi.repository.RunCaseRepository;
import com.promptops.platformapi.repository.RunRepository;
import com.promptops.platformapi.repository.RunTraceRepository;
import com.promptops.platformapi.repository.WorkflowRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetRunServiceImplTest {

  @Mock
  private ProjectRepository projectRepository;
  @Mock
  private DatasetRepository datasetRepository;
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
  private RunRequestedPublisher runRequestedPublisher;
  @Mock
  private RunExecutionService runExecutionService;

  @Test
  void startRun_createsQueuedRunAndPublishesEventWithoutExecutingCases() {
    Project project = new Project();
    project.setId(1L);
    Dataset dataset = new Dataset();
    dataset.setId(3L);
    dataset.setProjectId(1L);
    Workflow workflow = new Workflow();
    workflow.setId(7L);
    workflow.setProjectId(1L);
    workflow.setTemplateId("rag_json");
    DatasetItem item = new DatasetItem();
    item.setId(5L);
    item.setDatasetId(3L);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(datasetRepository.findByIdAndProjectId(3L, 1L)).thenReturn(Optional.of(dataset));
    when(workflowRepository.findByIdAndProjectId(7L, 1L)).thenReturn(Optional.of(workflow));
    when(datasetItemRepository.findByDatasetIdOrderByIdAsc(3L)).thenReturn(List.of(item));
    when(runRepository.save(any(Run.class))).thenAnswer(invocation -> {
      Run run = invocation.getArgument(0);
      run.setId(8L);
      return run;
    });

    DatasetRunServiceImpl service = new DatasetRunServiceImpl(
        projectRepository,
        datasetRepository,
        datasetItemRepository,
        workflowRepository,
        runRepository,
        runCaseRepository,
        runTraceRepository,
        runRequestedPublisher,
        runExecutionService,
        false);

    RunRequest request = new RunRequest();
    request.setWorkflowId(7L);
    request.setDatasetId(3L);
    request.setSchemaId("qa_answer");

    Run run = service.startRun(1L, request);

    assertThat(run.getStatus()).isEqualTo("QUEUED");
    assertThat(run.getTotalCases()).isEqualTo(1);
    assertThat(run.getSuccessCases()).isZero();
    assertThat(run.getFailedCases()).isZero();

    ArgumentCaptor<RunRequestedEvent> eventCaptor = ArgumentCaptor.forClass(RunRequestedEvent.class);
    verify(runRequestedPublisher).publish(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getRunId()).isEqualTo(8L);
    assertThat(eventCaptor.getValue().getProjectId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().getWorkflowId()).isEqualTo(7L);
    assertThat(eventCaptor.getValue().getDatasetId()).isEqualTo(3L);
    assertThat(eventCaptor.getValue().getSchemaId()).isEqualTo("qa_answer");

    verify(runExecutionService, never()).execute(any());
    verify(runCaseRepository, never()).save(any(RunCase.class));
  }
}
