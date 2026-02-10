"""
POST /retrieve endpoint.

Searches the knowledge base for chunks relevant to a query,
and optionally generates an LLM answer based on the retrieved context.
"""

import openai
from fastapi import APIRouter, Depends

from ai_runtime.config import Settings
from ai_runtime.models import RetrieveRequest, RetrieveResponse, ChunkResult
from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.dependencies import get_milvus_service, get_embedding_service, get_settings

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
    # TODO(human): Implement the retrieve logic
    # Hints:
    #
    # Step 1: Embed the query
    #   query_vector = embedding_svc.embed_single(request.query)
    #
    # Step 2: Search Milvus
    #   raw_results = milvus.search(request.project_id, query_vector, request.top_k)
    #   Convert raw_results (list of dicts) to list of ChunkResult objects:
    #   results = [ChunkResult(**r) for r in raw_results]
    #
    # Step 3 (optional): Generate LLM answer
    #   If results is not empty, call OpenAI chat to generate an answer:
    #     client = openai.OpenAI(api_key=settings.openai_api_key)
    #     context = "\n\n".join([f"[Source: {r.title}]\n{r.text}" for r in results])
    #     response = client.chat.completions.create(
    #         model=settings.openai_chat_model,
    #         messages=[
    #             {"role": "system", "content": "Answer based ONLY on the provided context. Cite source titles."},
    #             {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {request.query}"},
    #         ],
    #     )
    #     answer = response.choices[0].message.content
    #   If results is empty, set answer = None
    #
    # Step 4: Return RetrieveResponse(
    #     project_id=request.project_id,
    #     query=request.query,
    #     results=results,
    #     answer=answer,
    # )
    raise NotImplementedError("TODO: implement retrieve")
