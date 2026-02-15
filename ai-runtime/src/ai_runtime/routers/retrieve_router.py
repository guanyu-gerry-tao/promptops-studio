"""
POST /retrieve endpoint.

Searches the knowledge base for chunks relevant to a query,
and optionally generates an LLM answer based on the retrieved context.

Error handling: EmbeddingError and MilvusError bubble up to global
exception handlers in main.py. The LLM answer generation has its own
try/except because a failed answer should NOT fail the whole search —
the user still gets the retrieved chunks, just without an LLM answer.
"""

import logging

import openai
from fastapi import APIRouter, Depends

from ai_runtime.config import Settings
from ai_runtime.models import RetrieveRequest, RetrieveResponse, ChunkResult
from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.dependencies import get_milvus_service, get_embedding_service, get_settings

logger = logging.getLogger(__name__)

router = APIRouter(tags=["retrieval"])


@router.post("/retrieve", response_model=RetrieveResponse)
def retrieve(
    request: RetrieveRequest,
    milvus: MilvusService = Depends(get_milvus_service),
    embedding_svc: EmbeddingService = Depends(get_embedding_service),
    settings: Settings = Depends(get_settings),
) -> RetrieveResponse:
    """
    Search the knowledge base and optionally generate an answer.

    Flow:
      1. Embed the query text → get a vector
      2. Search Milvus for similar vectors → get relevant chunks
      3. (Optional) Send chunks + query to OpenAI chat → get a human-readable answer
    """
    logger.info("POST /retrieve: project=%d, query='%s'", request.project_id, request.query[:80])

    # Step 1 & 2: Embed query + search Milvus
    # (EmbeddingError / MilvusError will bubble up to global handlers)
    query_vector = embedding_svc.embed_single(request.query)
    top_k = request.top_k or settings.retrieve_top_k

    raw_results = milvus.search(
        project_id=request.project_id,
        query_embedding=query_vector,
        top_k=top_k,
    )
    results = [ChunkResult(**r) for r in raw_results]
    logger.info("Retrieved %d chunks for project %d", len(results), request.project_id)

    # Step 3: Optional LLM answer generation
    # This has its own try/except — if the LLM fails, we still return
    # the search results. The user just won't get a generated answer.
    answer: str | None = None
    if request.generate_answer and results:
        try:
            client = openai.OpenAI(api_key=settings.openai_api_key)
            context = "\n\n".join(
                [f"[Source: {r.title}]\n{r.text}" for r in results]
            )
            response = client.chat.completions.create(
                model=settings.openai_chat_model,
                messages=[
                    {
                        "role": "system",
                        "content": "Answer based ONLY on the provided context. Cite source titles.",
                    },
                    {
                        "role": "user",
                        "content": f"Context:\n{context}\n\nQuestion: {request.query}",
                    },
                ],
            )
            answer = response.choices[0].message.content
            logger.info("LLM answer generated successfully")
        except Exception as e:
            logger.warning("LLM answer generation failed (returning results without answer): %s", e)
            answer = None

    return RetrieveResponse(
        project_id=request.project_id,
        query=request.query,
        results=results,
        answer=answer,
    )
