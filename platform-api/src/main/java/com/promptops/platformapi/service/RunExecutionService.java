package com.promptops.platformapi.service;

import com.promptops.platformapi.dto.RunRequestedEvent;
import com.promptops.platformapi.entity.Run;

public interface RunExecutionService {

  Run execute(RunRequestedEvent event);
}
