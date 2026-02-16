package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.KbDoc;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.KbDocRepository;
import com.promptops.platformapi.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link KbDocService}.
 *
 * This service is the bridge between the Spring Boot world and the Python ai-runtime.
 * It saves document metadata to MySQL, then uses HTTP to tell ai-runtime
 * to process the document (chunk → embed → store in Milvus).
 */
@Service
public class KbDocServiceImpl implements KbDocService {

    private static final Logger log = LoggerFactory.getLogger(KbDocServiceImpl.class);

    private final KbDocRepository kbDocRepository;
    private final ProjectRepository projectRepository;
    private final RestClient restClient;

    /**
     * Constructor injection.
     *
     * @Value reads "ai-runtime.base-url" from application.yml.
     * RestClient is Spring's modern HTTP client (replaces the older RestTemplate).
     * Think of it as Java's equivalent of Python's "requests" library.
     */
    public KbDocServiceImpl(
            KbDocRepository kbDocRepository,
            ProjectRepository projectRepository,
            @Value("${ai-runtime.base-url}") String aiRuntimeBaseUrl) {
        this.kbDocRepository = kbDocRepository;
        this.projectRepository = projectRepository;
        // Force HTTP/1.1 because uvicorn (ai-runtime) does not support HTTP/2.
        // JDK 21's HttpClient defaults to HTTP/2, which causes "Invalid HTTP request" errors.
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
    public KbDoc uploadAndIndex(Long projectId, String title, String content) {
        // 1. Verify project exists
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Project not found with id: " + projectId));

        // 2. Save document metadata to MySQL with status = INDEXING
        KbDoc doc = new KbDoc();
        doc.setProjectId(projectId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setStatus("INDEXING");
        doc.setChunksCount(0);
        doc = kbDocRepository.save(doc);

        log.info("Saved KbDoc id={} for project={}, sending to ai-runtime", doc.getId(), projectId);

        // 3. Call ai-runtime POST /index
        try {
            Map<String, Object> requestBody = Map.of(
                    "project_id", projectId,
                    "doc_id", doc.getId(),
                    "title", title,
                    "content", content
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 4. Update status based on response
            String status = (String) response.get("status");
            if ("SUCCESS".equals(status)) {
                Integer chunksCount = (Integer) response.get("chunks_count");
                doc.setStatus("INDEXED");
                doc.setChunksCount(chunksCount);
                doc.setErrorMessage(null);
                log.info("KbDoc id={} indexed successfully: {} chunks", doc.getId(), chunksCount);
            } else {
                String message = (String) response.get("message");
                doc.setStatus("FAILED");
                doc.setErrorMessage(message);
                log.error("KbDoc id={} indexing failed: {}", doc.getId(), message);
            }

        } catch (Exception e) {
            // ai-runtime is down or returned an error
            doc.setStatus("FAILED");
            doc.setErrorMessage("ai-runtime call failed: " + e.getMessage());
            log.error("Failed to call ai-runtime for KbDoc id={}: {}", doc.getId(), e.getMessage());
        }

        return kbDocRepository.save(doc);
    }

    @Override
    public List<KbDoc> findByProjectId(Long projectId) {
        return kbDocRepository.findByProjectId(projectId);
    }

    @Override
    public KbDoc findById(Long id) {
        return kbDocRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "KbDoc not found with id: " + id));
    }

    @Override
    public void delete(Long id) {
        KbDoc doc = findById(id);
        kbDocRepository.delete(doc);
        log.info("Deleted KbDoc id={}", id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> search(Long projectId, String query, Integer topK, Boolean generateAnswer) {
        // Verify project exists
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "Project not found with id: " + projectId));

        log.info("Searching KB for project={}, query='{}'", projectId, query);

        Map<String, Object> requestBody = Map.of(
                "project_id", projectId,
                "query", query,
                "top_k", topK != null ? topK : 5,
                "generate_answer", generateAnswer != null ? generateAnswer : true
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            log.info("KB search returned results for project={}", projectId);
            return response;

        } catch (Exception e) {
            log.error("KB search failed for project={}: {}", projectId, e.getMessage());
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY, "ai-runtime search failed: " + e.getMessage());
        }
    }
}
