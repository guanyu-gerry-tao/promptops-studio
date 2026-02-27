package com.promptops.platformapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Knowledge Base Document entity.
 *
 * Maps to the "kb_docs" table in the database.
 * Each KbDoc represents one document uploaded to a project's knowledge base.
 *
 * This table stores METADATA only — the actual chunked content and
 * embedding vectors live in Milvus (managed by ai-runtime).
 */
@Entity
@Table(name = "kb_docs")
@Data
public class KbDoc {

    /**
     * Document ID - primary key, auto-incremented.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which project this document belongs to.
     * Maps to projects.id via foreign key.
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * Document title (e.g. "Employee Handbook")
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Original document content in Markdown format.
     * Stored here so we can re-index without re-uploading.
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    /**
     * Indexing status: PENDING → INDEXING → INDEXED or FAILED.
     *
     * PENDING:  just uploaded, not yet sent to ai-runtime
     * INDEXING: sent to ai-runtime, waiting for response
     * INDEXED:  ai-runtime successfully processed and stored in Milvus
     * FAILED:   ai-runtime returned an error
     */
    @Column(length = 20)
    private String status;

    /**
     * Number of chunks created in Milvus after indexing.
     * 0 if not yet indexed or if indexing failed.
     */
    @Column(name = "chunks_count")
    private Integer chunksCount;

    /**
     * Error details if indexing failed. Null on success.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * When this document was uploaded.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * When this record was last updated (e.g. status change).
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
