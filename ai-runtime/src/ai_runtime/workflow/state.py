"""
Workflow state definition for LangGraph.

The State is a TypedDict that flows through every node in the graph.
Each node reads what it needs, does its work, and writes its outputs
back into the state. LangGraph merges the returned dict into the
existing state automatically (like a reducer).

This is the "shared blackboard" pattern — all nodes communicate
through a single, well-typed state object instead of passing
arguments directly to each other.
"""

from typing import TypedDict


class ChunkDict(TypedDict):
    """A retrieved chunk — same shape as WeaviateService returns."""
    doc_id: int
    chunk_id: int
    title: str
    text: str
    score: float


class TraceEntry(TypedDict):
    """One node's execution trace record."""
    node_name: str
    input_summary: str
    output_summary: str
    latency_ms: int
    token_count: int | None
    citations: list[str] | None


class WorkflowState(TypedDict, total=False):
    """
    ★ THE central state object that every node reads and writes.

    Fields marked total=False means they're all optional — nodes only
    need to return the keys they want to update. LangGraph merges them.

    Lifecycle:
      1. Router creates initial state with project_id, user_input, etc.
      2. route_intent → sets needs_retrieval
      3. retrieve_kb → sets retrieved_chunks
      4. generate_json → sets draft_output_json
      5. validate_schema → sets validation_passed / validation_errors
      6. repair_json → overwrites draft_output_json (if repair needed)
      7. finalize → sets final_output_json, status
    """

    # --- Input (set once by execute_router, never modified by nodes) ---
    project_id: int              # Project ID, scopes KB retrieval to this project
    workflow_id: str             # Workflow template ID (MVP: always "rag_json")
    case_id: str                 # Test case ID for tracking and logging
    user_input: str              # Original user question, used by retrieval and generation
    schema_id: str | None        # Output JSON Schema ID (e.g. "qa_answer"); None = skip validation

    # --- Retrieval (written by route_intent & retrieve_kb) ---
    needs_retrieval: bool        # Whether this query needs KB retrieval (set by route_intent)
    retrieved_chunks: list[ChunkDict]  # KB chunks returned by retrieve_kb (hybrid search + rerank)

    # --- Generation (written by generate_json, may be overwritten by repair_json) ---
    draft_output_json: dict | None  # LLM-generated JSON draft; repair_json overwrites on retry

    # --- Validation (written by validate_schema) ---
    validation_passed: bool      # Whether draft_output_json passes JSON Schema validation
    validation_errors: list[str] # Validation error messages (empty if passed)
    repair_attempts: int         # Number of repair attempts so far; max 2 before forced finalize

    # --- Final output (written by finalize) ---
    final_output_json: dict | None  # Promoted from draft on success; None on failure
    status: str                  # Execution result: "SUCCESS" or "FAILED"
    error_message: str | None    # Error description on failure; None on success

    # --- Execution trace (appended by every node) ---
    trace: list[TraceEntry]      # Per-node execution records (latency, tokens, citations)
