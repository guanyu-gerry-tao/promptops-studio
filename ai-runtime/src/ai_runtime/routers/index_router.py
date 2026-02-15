"""
POST /index endpoint.

Called by Platform API after a document is uploaded.
Chunks the document, generates embeddings, and stores them in Milvus.

Error handling: Exceptions from services (EmbeddingError, MilvusError)
are NOT caught here — they bubble up to the global exception handlers
in main.py, which return clean JSON error responses.
"""

import logging

from fastapi import APIRouter, Depends

from ai_runtime.models import IndexRequest, IndexResponse
from ai_runtime.services.document_service import DocumentService
from ai_runtime.dependencies import get_document_service

logger = logging.getLogger(__name__)

router = APIRouter(tags=["indexing"])


@router.post("/index", response_model=IndexResponse)
def index_document(
    request: IndexRequest,
    doc_service: DocumentService = Depends(get_document_service),
) -> IndexResponse:
    """
    Index a document into the vector database.

    Flow: receive document → chunk → embed → store in Milvus
    """
    logger.info("POST /index: project=%d, doc_id=%d", request.project_id, request.doc_id)

    chunks_count = doc_service.process_document(
        project_id=request.project_id,
        doc_id=request.doc_id,
        title=request.title,
        content=request.content,
    )
    return IndexResponse(
        project_id=request.project_id,
        doc_id=request.doc_id,
        chunks_count=chunks_count,
        status="SUCCESS",
        message=f"Indexed {chunks_count} chunks for document {request.doc_id}",
    )
