"""
POST /retrieve-document endpoint.

All searches go through Weaviate hybrid search (vector + BM25).
alpha controls the blend: 0.0 = pure keyword, 1.0 = pure vector, default = 0.5.

# MILVUS (dead code — kept for rollback):
# MilvusService and get_milvus_service are imported in dependencies.py
# but not injected here. To re-enable: add the Depends parameter back.
"""

import logging

import openai
from fastapi import APIRouter, Depends

from ai_runtime.config import Settings
from ai_runtime.models import RetrieveRequest, RetrieveResponse, ChunkResult
from ai_runtime.services.weaviate_service import WeaviateService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.dependencies import (
    get_weaviate_service,
    get_embedding_service,
    get_settings,
)

logger = logging.getLogger(__name__)

router = APIRouter(tags=["retrieval"])


@router.post("/retrieve-document", response_model=RetrieveResponse)
def retrieve(
    request: RetrieveRequest,
    weaviate_svc: WeaviateService = Depends(get_weaviate_service),
    embedding_svc: EmbeddingService = Depends(get_embedding_service),
    settings: Settings = Depends(get_settings),
) -> RetrieveResponse:
    """
    Search the knowledge base and optionally generate an answer.

    Flow:
      1. Embed the query text → get a vector
      2. Weaviate hybrid search (vector + BM25, blended by alpha)
      3. (Optional) Send chunks + query to OpenAI chat → get a human-readable answer
    """
    logger.info(
        "POST /retrieve-document: project=%d, query='%s', alpha=%s",
        request.project_id, request.query[:80], request.alpha,
    )

    # Step 1: Embed the query (needed by both Milvus and Weaviate)
    query_vector = embedding_svc.embed_single(request.query)
    top_k = request.top_k or settings.retrieve_top_k

    # Step 2: Weaviate hybrid search (default path)
    # Milvus code is kept but no longer routed to.
    alpha = max(0.0, min(1.0, request.alpha if request.alpha is not None else settings.weaviate_alpha))
    logger.info("Using Weaviate hybrid search (alpha=%.2f)", alpha)
    raw_results = weaviate_svc.hybrid_search(
        project_id=request.project_id,
        query=request.query,
        query_embedding=query_vector,
        alpha=alpha,
        top_k=top_k,
    )

    results = [ChunkResult(**r) for r in raw_results]
    logger.info("Retrieved %d chunks for project %d", len(results), request.project_id)

    # Step 3: Optional LLM answer generation
    # If LLM fails, we still return the search results (just without an answer).
    answer: str | None = None
    if request.generate_answer and results:
        try:
            client = openai.OpenAI(api_key=settings.openai_api_key)

            context = "\n\n".join(
                [f"[Source: {r.title}]\n{r.text}" for r in results]
            )

            # --- Prompts (edit here to tune LLM behavior) ---
            system_prompt = (
                "You are a helpful assistant that answers questions strictly based on "
                "the provided context documents.\n\n"
                "## Rules\n"
                "- Answer ONLY from the context below. Do not use outside knowledge.\n"
                "- Cite the source title(s) at the end of your answer, e.g. *Source: Title*.\n"
                "- If the context does not contain enough information, say so clearly."
            )

            user_prompt = (
                f"## Context\n\n{context}\n\n"
                f"## Question\n\n{request.query}"
            )
            # --- End prompts ---

            response = client.chat.completions.create(
                model=settings.openai_chat_model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user",   "content": user_prompt},
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
