package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.KbDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for KbDoc entity.
 *
 * Same pattern as ProjectRepository — Spring Data JPA generates
 * the SQL queries automatically from method names.
 */
@Repository
public interface KbDocRepository extends JpaRepository<KbDoc, Long> {

    /**
     * Find all documents belonging to a project.
     * Used by: "list all docs in this project's knowledge base"
     *
     * Generated SQL: SELECT * FROM kb_docs WHERE project_id = ?
     */
    List<KbDoc> findByProjectId(Long projectId);

    /**
     * Find documents by project and status.
     * Used by: "show me all successfully indexed docs"
     *
     * Generated SQL: SELECT * FROM kb_docs WHERE project_id = ? AND status = ?
     */
    List<KbDoc> findByProjectIdAndStatus(Long projectId, String status);
}
