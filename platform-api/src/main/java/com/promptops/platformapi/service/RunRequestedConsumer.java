package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.RunRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "promptops.worker.enabled", havingValue = "true")
public class RunRequestedConsumer {

  private static final Logger log = LoggerFactory.getLogger(RunRequestedConsumer.class);

  private final RunExecutionService runExecutionService;

  public RunRequestedConsumer(RunExecutionService runExecutionService) {
    this.runExecutionService = runExecutionService;
  }

  @KafkaListener(topics = "${kafka.topic.run-requested}")
  public void handle(RunRequestedEvent event) {
    log.info("run.requested consumed runId={} projectId={} workflowId={} datasetId={}",
        event.getRunId(), event.getProjectId(), event.getWorkflowId(), event.getDatasetId());
    runExecutionService.execute(event);
  }
}
