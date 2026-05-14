package com.promptops.platformapi.service;

import java.net.http.HttpClient;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestAiRuntimeClient implements AiRuntimeClient {

  private final RestClient restClient;

  public RestAiRuntimeClient(@Value("${ai-runtime.base-url}") String aiRuntimeBaseUrl) {
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
  @SuppressWarnings("unchecked")
  public Map<String, Object> executeCase(Map<String, Object> requestBody) {
    return restClient.post()
        .uri("/execute-case")
        .contentType(MediaType.APPLICATION_JSON)
        .body(requestBody)
        .retrieve()
        .body(Map.class);
  }
}
