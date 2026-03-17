"""
Pydantic models for API request and response schemas.

These are the Python equivalent of Java DTOs (Data Transfer Objects).
FastAPI uses these to:
  1. Validate incoming JSON automatically (like @Valid in Spring)
  2. Generate Swagger documentation (like SpringDoc/OpenAPI)
  3. Serialize response objects to JSON
"""

from pydantic import BaseModel


# ──────────────────────────────────────
# /index endpoint
# ──────────────────────────────────────

class IndexRequest(BaseModel):
    """Request body for POST /index-document — index a document into Milvus."""
    project_id: int
    doc_id: int
    title: str
    content: str  # Full markdown text to chunk and index


class IndexResponse(BaseModel):
    """Response body for POST /index-document."""
    project_id: int
    doc_id: int
    chunks_count: int
    status: str    # "SUCCESS" or "FAILED"
    message: str


# ──────────────────────────────────────
# /retrieve-document endpoint
# ──────────────────────────────────────

class RetrieveRequest(BaseModel):
    """Request body for POST /retrieve-document — search the knowledge base."""
    project_id: int
    query: str
    top_k: int = 5              # Default: return top 5 results
    generate_answer: bool = True  # Whether to call LLM to generate a final answer
    alpha: float | None = None  # Hybrid search blend: 0.0=BM25, 1.0=vector, None=use Milvus (pure vector)


class ChunkResult(BaseModel):
    """A single search result (one chunk from a document)."""
    doc_id: int
    chunk_id: int
    text: str
    score: float   # Similarity score (0.0 ~ 1.0, higher = more similar)
    title: str     # Source document title, for citation


class RetrieveResponse(BaseModel):
    """Response body for POST /retrieve-document."""
    project_id: int
    query: str
    results: list[ChunkResult]
    answer: str | None = None  # Optional LLM-generated answer


# ──────────────────────────────────────
# /execute-case endpoint
# ──────────────────────────────────────

class ExecuteCaseRequest(BaseModel):
    """Request body for POST /execute-case — run a single case through a workflow."""
    project_id: int
    workflow_id: str            # Template identifier, e.g. "rag_json"
    case_id: str                # Unique identifier for this test case
    user_input: str             # The user query / test input
    schema_id: str | None = None  # Optional JSON Schema name for output validation


class TraceRecord(BaseModel):
    """One node's execution record in the workflow trace."""
    node_name: str
    input_summary: str          # Abbreviated input for this node
    output_summary: str         # Abbreviated output from this node
    latency_ms: int             # Time spent in this node
    token_count: int | None = None  # Token usage (if applicable)
    citations: list[str] | None = None  # Source document titles cited


class ExecuteCaseResponse(BaseModel):
    """Response body for POST /execute-case."""
    project_id: int
    workflow_id: str
    case_id: str
    status: str                 # "SUCCESS" or "FAILED"
    output_json: dict | None = None  # The structured JSON output from the workflow
    error_message: str | None = None
    trace: list[TraceRecord]    # Node-level execution trace
    citations: list[str]        # Aggregated source titles
