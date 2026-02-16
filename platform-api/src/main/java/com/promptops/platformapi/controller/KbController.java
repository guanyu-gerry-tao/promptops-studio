package com.promptops.platformapi.controller;

import com.promptops.platformapi.dto.KbDocRequest;
import com.promptops.platformapi.dto.KbSearchRequest;
import com.promptops.platformapi.entity.KbDoc;
import com.promptops.platformapi.service.KbDocService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Knowledge Base controller.
 *
 * Provides endpoints for managing documents in a project's knowledge base.
 * All paths are nested under /projects/{projectId}/kb to enforce
 * that every KB operation is scoped to a specific project.
 *
 * API design matches the plan:
 *   POST   /projects/{id}/kb/docs     → upload document + trigger index
 *   GET    /projects/{id}/kb/docs     → list documents
 *   GET    /projects/{id}/kb/docs/{docId} → get single document
 *   DELETE /projects/{id}/kb/docs/{docId} → delete document
 *   POST   /projects/{id}/kb/search   → search knowledge base
 */
@RestController
@RequestMapping("/projects/{projectId}/kb")
public class KbController {

    private final KbDocService kbDocService;

    public KbController(KbDocService kbDocService) {
        this.kbDocService = kbDocService;
    }

    /**
     * Upload a document and trigger indexing.
     * POST /projects/{projectId}/kb/docs
     */
    @PostMapping("/docs")
    public ResponseEntity<KbDoc> uploadDoc(
            @PathVariable Long projectId,
            @Valid @RequestBody KbDocRequest request) {
        KbDoc doc = kbDocService.uploadAndIndex(projectId, request.getTitle(), request.getContent());
        return ResponseEntity.ok(doc);
    }

    /**
     * List all documents in this project's knowledge base.
     * GET /projects/{projectId}/kb/docs
     */
    @GetMapping("/docs")
    public ResponseEntity<List<KbDoc>> listDocs(@PathVariable Long projectId) {
        List<KbDoc> docs = kbDocService.findByProjectId(projectId);
        return ResponseEntity.ok(docs);
    }

    /**
     * Get a single document by ID.
     * GET /projects/{projectId}/kb/docs/{docId}
     */
    @GetMapping("/docs/{docId}")
    public ResponseEntity<KbDoc> getDoc(
            @PathVariable Long projectId,
            @PathVariable Long docId) {
        KbDoc doc = kbDocService.findById(docId);
        return ResponseEntity.ok(doc);
    }

    /**
     * Delete a document.
     * DELETE /projects/{projectId}/kb/docs/{docId}
     */
    @DeleteMapping("/docs/{docId}")
    public ResponseEntity<Void> deleteDoc(
            @PathVariable Long projectId,
            @PathVariable Long docId) {
        kbDocService.delete(docId);
        return ResponseEntity.ok().build();
    }

    /**
     * Search the knowledge base.
     * POST /projects/{projectId}/kb/search
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @PathVariable Long projectId,
            @Valid @RequestBody KbSearchRequest request) {
        Map<String, Object> result = kbDocService.search(
                projectId, request.getQuery(), request.getTopK(), request.getGenerateAnswer());
        return ResponseEntity.ok(result);
    }
}
