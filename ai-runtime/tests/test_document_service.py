"""
Unit tests for DocumentService.

DocumentService is the "orchestrator" — it calls EmbeddingService,
MilvusService, and WeaviateService. We mock all three dependencies
and verify the orchestration logic: splitting, dual-write to both
stores, and error propagation.
"""

import pytest
from unittest.mock import Mock, patch

from ai_runtime.services.document_service import DocumentService
from ai_runtime.exceptions import (
    EmbeddingError,
    MilvusError,
    WeaviateError,
    DocumentProcessingError,
    AIRuntimeError,
)


@pytest.fixture
def mock_milvus():
    return Mock()


@pytest.fixture
def mock_weaviate():
    return Mock()


@pytest.fixture
def mock_embedding():
    return Mock()


@pytest.fixture
def doc_service(mock_milvus, mock_weaviate, mock_embedding, fake_settings):
    """DocumentService with all three dependencies mocked."""
    return DocumentService(
        milvus_service=mock_milvus,
        weaviate_service=mock_weaviate,
        embedding_service=mock_embedding,
        settings=fake_settings,
    )


class TestProcessDocument:
    """Tests for DocumentService.process_document()."""

    def test_happy_path_writes_to_weaviate(
        self, doc_service, mock_milvus, mock_weaviate, mock_embedding
    ):
        """Full pipeline: content → split → embed → store in Weaviate. Milvus not called."""
        mock_embedding.embed_texts.return_value = [[0.1] * 1536, [0.2] * 1536]

        result = doc_service.process_document(
            project_id=1,
            doc_id=10,
            title="Test Doc",
            content="This is a fairly long test document. " * 20,
        )

        mock_embedding.embed_texts.assert_called_once()
        chunk_texts = mock_embedding.embed_texts.call_args[0][0]
        assert len(chunk_texts) > 0

        # Milvus is dead code — must NOT be called
        mock_milvus.insert_chunks.assert_not_called()

        # Weaviate receives the data
        mock_weaviate.insert_chunks.assert_called_once()
        weaviate_kwargs = mock_weaviate.insert_chunks.call_args[1]
        assert weaviate_kwargs["project_id"] == 1
        assert all(d == 10 for d in weaviate_kwargs["doc_ids"])
        assert weaviate_kwargs["chunk_ids"] == list(range(len(chunk_texts)))

        assert result == len(chunk_texts)

    def test_empty_content_returns_zero(self, doc_service, mock_embedding, mock_milvus, mock_weaviate):
        """Empty content → 0 chunks, no calls to embedding or either store."""
        mock_embedding.embed_texts.return_value = []

        result = doc_service.process_document(
            project_id=1, doc_id=10, title="Empty Doc", content="",
        )

        assert result == 0
        mock_embedding.embed_texts.assert_not_called()
        mock_milvus.insert_chunks.assert_not_called()
        mock_weaviate.insert_chunks.assert_not_called()

    def test_propagates_embedding_error(self, doc_service, mock_embedding):
        """EmbeddingError bubbles up unchanged — neither store is written."""
        mock_embedding.embed_texts.side_effect = EmbeddingError("OpenAI is down")

        with pytest.raises(EmbeddingError, match="OpenAI is down"):
            doc_service.process_document(
                project_id=1, doc_id=10, title="Doc", content="Some content " * 50,
            )

    # test_propagates_milvus_error removed — Milvus insert is dead code, never called.

    def test_propagates_weaviate_error(self, doc_service, mock_embedding, mock_weaviate):
        """WeaviateError from Weaviate insert bubbles up unchanged."""
        mock_embedding.embed_texts.return_value = [[0.1] * 1536]
        mock_weaviate.insert_chunks.side_effect = WeaviateError("Weaviate is down")

        with pytest.raises(WeaviateError, match="Weaviate is down"):
            doc_service.process_document(
                project_id=1, doc_id=10, title="Doc", content="Some content " * 50,
            )

    def test_wraps_unexpected_error_as_document_processing_error(
        self, doc_service, mock_embedding
    ):
        """Non-AIRuntimeError exceptions get wrapped as DocumentProcessingError."""
        mock_embedding.embed_texts.side_effect = ValueError("unexpected")

        with pytest.raises(DocumentProcessingError, match="Failed to process"):
            doc_service.process_document(
                project_id=1, doc_id=10, title="Doc", content="Some content " * 50,
            )


class TestDeleteDocument:
    """Tests for DocumentService.delete_document()."""

    def test_delegates_to_weaviate(self, doc_service, mock_milvus, mock_weaviate):
        """delete_document calls Weaviate only. Milvus is dead code."""
        doc_service.delete_document(project_id=1, doc_id=10)

        mock_milvus.delete_by_doc_id.assert_not_called()
        mock_weaviate.delete_by_doc_id.assert_called_once_with(1, 10)
