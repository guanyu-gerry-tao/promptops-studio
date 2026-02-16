package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.KbDoc;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Knowledge Base document operations.
 *
 * Same "Interface + Impl" pattern as ProjectService.
 */
public interface KbDocService {

    /**
     * Upload a document and trigger indexing via ai-runtime.
     *
     * Flow:
     *   1. Save document metadata to MySQL (status = INDEXING)
     *   2. Call ai-runtime POST /index with the content
     *   3. Update status to INDEXED (success) or FAILED (error)
     */
    KbDoc uploadAndIndex(Long projectId, String title, String content);

    /**
     * List all documents in a project's knowledge base.
     */
    List<KbDoc> findByProjectId(Long projectId);

    /**
     * Get a single document by ID.
     */
    KbDoc findById(Long id);

    /**
     * Delete a document (removes from MySQL; Milvus cleanup is TODO).
     */
    void delete(Long id);

    /**
     * Search the knowledge base via ai-runtime POST /retrieve.
     * Returns the raw response from ai-runtime as a Map.
     */
    Map<String, Object> search(Long projectId, String query, Integer topK, Boolean generateAnswer);
}
