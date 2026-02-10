"""
POST /index endpoint.

Called by Platform API after a document is uploaded.
Chunks the document, generates embeddings, and stores them in Milvus.
"""

from fastapi import APIRouter, Depends

from ai_runtime.models import IndexRequest, IndexResponse
from ai_runtime.services.document_service import DocumentService
from ai_runtime.dependencies import get_document_service

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
    # TODO(human): Implement the endpoint logic
    # Hints:
    #   1. Call doc_service.process_document(...) with the request fields
    #   2. Wrap in try/except to catch errors
    #   3. Return IndexResponse with status "SUCCESS" and chunks_count
    #   4. On error, return IndexResponse with status "FAILED" and the error message
    #
    # Example success response:
    #   IndexResponse(
    #       project_id=request.project_id,
    #       doc_id=request.doc_id,
    #       chunks_count=7,
    #       status="SUCCESS",
    #       message="Indexed 7 chunks for document 42",
    #   )
    raise NotImplementedError("TODO: implement index_document")
