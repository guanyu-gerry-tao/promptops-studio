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
    top_k: int = 5  # Default: return top 5 results
    generate_answer: bool = True  # Whether to call LLM to generate a final answer


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
