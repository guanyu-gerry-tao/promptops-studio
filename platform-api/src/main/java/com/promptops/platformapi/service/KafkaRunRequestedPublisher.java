package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.RunRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaRunRequestedPublisher implements RunRequestedPublisher {

  private final KafkaTemplate<String, RunRequestedEvent> kafkaTemplate;
  private final String topic;

  public KafkaRunRequestedPublisher(
      KafkaTemplate<String, RunRequestedEvent> kafkaTemplate,
      @Value("${kafka.topic.run-requested}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  public void publish(RunRequestedEvent event) {
    kafkaTemplate.send(topic, String.valueOf(event.getRunId()), event);
  }
}
