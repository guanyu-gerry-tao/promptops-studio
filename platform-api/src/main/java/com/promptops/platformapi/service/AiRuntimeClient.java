package com.promptops.platformapi.service;

import java.util.Map;

public interface AiRuntimeClient {

  Map<String, Object> executeCase(Map<String, Object> requestBody);
}
