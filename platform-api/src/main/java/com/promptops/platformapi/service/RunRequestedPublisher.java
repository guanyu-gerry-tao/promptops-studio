package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.RunRequestedEvent;

public interface RunRequestedPublisher {

  void publish(RunRequestedEvent event);
}
