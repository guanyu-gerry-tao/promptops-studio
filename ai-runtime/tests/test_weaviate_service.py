"""
Unit tests for WeaviateService.

Strategy: Mock weaviate.connect_to_local so we never need a real Weaviate
server. Each test patches only what it needs — the rest is real code running.

Mock structure:
    mock_client
    └── collections
        ├── exists(name)          → bool
        ├── create(...)           → None
        ├── get(name)             → mock_collection
        └── mock_collection
            ├── batch.dynamic()   → context manager → mock_batch
            │   └── add_object(properties, vector)
            ├── query.hybrid(...) → response with .objects
            └── data.delete_many(where=...)
"""

import pytest
from unittest.mock import patch, Mock, MagicMock

from ai_runtime.services.weaviate_service import WeaviateService
from ai_runtime.exceptions import WeaviateError


# ──────────────────────────────────────
# Fixtures
# ──────────────────────────────────────

@pytest.fixture
def mock_client():
    """A fully mocked weaviate client returned by connect_to_local."""
    return MagicMock()


@pytest.fixture
def mock_weaviate_service(fake_settings, mock_client):
    """
    WeaviateService with weaviate.connect_to_local patched.
    The service is connected but talks to mock_client instead of a real server.
    """
    with patch("ai_runtime.services.weaviate_service.weaviate.connect_to_local", return_value=mock_client):
        service = WeaviateService(fake_settings)
    return service


# ──────────────────────────────────────
# __init__
# ──────────────────────────────────────

class TestInit:
    def test_connection_failure_raises_weaviate_error(self, fake_settings):
        """If Weaviate is unreachable, constructor raises WeaviateError."""
        with patch(
            "ai_runtime.services.weaviate_service.weaviate.connect_to_local",
            side_effect=ConnectionError("refused"),
        ):
            with pytest.raises(WeaviateError, match="Cannot connect"):
                WeaviateService(fake_settings)


# ──────────────────────────────────────
# _collection_name
# ──────────────────────────────────────

class TestCollectionName:
    def test_returns_uppercase_kb_prefix(self, mock_weaviate_service):
        """Weaviate class names must start with uppercase — Kb1 not kb_1."""
        assert mock_weaviate_service._collection_name(1) == "Kb1"
        assert mock_weaviate_service._collection_name(42) == "Kb42"


# ──────────────────────────────────────
# ensure_collection
# ──────────────────────────────────────

class TestEnsureCollection:
    def test_skips_creation_if_already_exists(self, mock_weaviate_service, mock_client):
        """If collection Kb1 already exists, do not call collections.create."""
        mock_client.collections.exists.return_value = True

        mock_weaviate_service.ensure_collection(project_id=1)

        mock_client.collections.create.assert_not_called()

    def test_creates_collection_when_missing(self, mock_weaviate_service, mock_client):
        """If collection does not exist, create it with correct name."""
        mock_client.collections.exists.return_value = False

        mock_weaviate_service.ensure_collection(project_id=1)

        mock_client.collections.create.assert_called_once()
        call_kwargs = mock_client.collections.create.call_args[1]
        assert call_kwargs["name"] == "Kb1"

    def test_wraps_error_as_weaviate_error(self, mock_weaviate_service, mock_client):
        """Unexpected errors from Weaviate SDK are wrapped as WeaviateError."""
        mock_client.collections.exists.side_effect = RuntimeError("disk full")

        with pytest.raises(WeaviateError, match="Failed to create/access"):
            mock_weaviate_service.ensure_collection(project_id=1)


# ──────────────────────────────────────
# insert_chunks
# ──────────────────────────────────────

class TestInsertChunks:
    def test_happy_path_inserts_all_chunks(self, mock_weaviate_service, mock_client):
        """
        insert_chunks calls batch.add_object once per chunk with correct properties and vector.
        """
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection

        # batch.dynamic() is a context manager — set it up correctly
        mock_batch = MagicMock()
        mock_collection.batch.dynamic.return_value.__enter__ = Mock(return_value=mock_batch)
        mock_collection.batch.dynamic.return_value.__exit__ = Mock(return_value=False)

        result = mock_weaviate_service.insert_chunks(
            project_id=1,
            doc_ids=[10, 10],
            chunk_ids=[0, 1],
            titles=["Doc A", "Doc A"],
            texts=["hello", "world"],
            embeddings=[[0.1] * 1536, [0.2] * 1536],
        )

        assert result == 2
        assert mock_batch.add_object.call_count == 2

        # Verify first chunk's properties
        first_call_kwargs = mock_batch.add_object.call_args_list[0][1]
        assert first_call_kwargs["properties"]["doc_id"] == 10
        assert first_call_kwargs["properties"]["text"] == "hello"
        assert first_call_kwargs["vector"] == [0.1] * 1536

    def test_wraps_error_as_weaviate_error(self, mock_weaviate_service, mock_client):
        """If batch insert fails, wrap as WeaviateError."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection
        mock_collection.batch.dynamic.side_effect = RuntimeError("write failed")

        with pytest.raises(WeaviateError, match="Failed to insert"):
            mock_weaviate_service.insert_chunks(
                project_id=1,
                doc_ids=[10],
                chunk_ids=[0],
                titles=["Doc"],
                texts=["text"],
                embeddings=[[0.1] * 1536],
            )


# ──────────────────────────────────────
# hybrid_search
# ──────────────────────────────────────

class TestHybridSearch:
    def test_returns_empty_if_collection_missing(self, mock_weaviate_service, mock_client):
        """If project has no collection yet, return [] without error."""
        mock_client.collections.exists.return_value = False

        result = mock_weaviate_service.hybrid_search(
            project_id=999, query="test", query_embedding=[0.1] * 1536,
            alpha=0.5, top_k=5,
        )

        assert result == []

    def test_returns_formatted_results(self, mock_weaviate_service, mock_client):
        """Happy path: hybrid search results are formatted as list of dicts."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection

        # Build fake Weaviate response objects
        obj1 = Mock()
        obj1.properties = {"doc_id": 10, "chunk_id": 0, "title": "Doc A", "text": "hello"}
        obj1.metadata.score = 0.9

        obj2 = Mock()
        obj2.properties = {"doc_id": 10, "chunk_id": 1, "title": "Doc A", "text": "world"}
        obj2.metadata.score = 0.7

        mock_response = Mock()
        mock_response.objects = [obj1, obj2]
        mock_collection.query.hybrid.return_value = mock_response

        result = mock_weaviate_service.hybrid_search(
            project_id=1, query="hello world",
            query_embedding=[0.1] * 1536, alpha=0.5, top_k=5,
        )

        assert len(result) == 2
        assert result[0]["doc_id"] == 10
        assert result[0]["score"] == 0.9
        assert result[1]["text"] == "world"

    def test_passes_alpha_to_query(self, mock_weaviate_service, mock_client):
        """alpha value is forwarded to Weaviate's hybrid() call unchanged."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection
        mock_collection.query.hybrid.return_value = Mock(objects=[])

        mock_weaviate_service.hybrid_search(
            project_id=1, query="test",
            query_embedding=[0.1] * 1536, alpha=0.3, top_k=5,
        )

        call_kwargs = mock_collection.query.hybrid.call_args[1]
        assert call_kwargs["alpha"] == 0.3

    def test_wraps_error_as_weaviate_error(self, mock_weaviate_service, mock_client):
        """Weaviate SDK errors are wrapped as WeaviateError."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection
        mock_collection.query.hybrid.side_effect = RuntimeError("query timeout")

        with pytest.raises(WeaviateError, match="Hybrid search failed"):
            mock_weaviate_service.hybrid_search(
                project_id=1, query="test",
                query_embedding=[0.1] * 1536, alpha=0.5, top_k=5,
            )


# ──────────────────────────────────────
# delete_by_doc_id
# ──────────────────────────────────────

class TestDeleteByDocId:
    def test_skips_if_collection_missing(self, mock_weaviate_service, mock_client):
        """No collection → no error, just skip silently."""
        mock_client.collections.exists.return_value = False

        # Should not raise
        mock_weaviate_service.delete_by_doc_id(project_id=999, doc_id=10)

        mock_client.collections.get.assert_not_called()

    def test_deletes_by_doc_id_filter(self, mock_weaviate_service, mock_client):
        """Happy path: delete_many is called with correct doc_id filter."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection

        mock_weaviate_service.delete_by_doc_id(project_id=1, doc_id=42)

        mock_collection.data.delete_many.assert_called_once()

    def test_wraps_error_as_weaviate_error(self, mock_weaviate_service, mock_client):
        """SDK errors during delete are wrapped as WeaviateError."""
        mock_client.collections.exists.return_value = True
        mock_collection = MagicMock()
        mock_client.collections.get.return_value = mock_collection
        mock_collection.data.delete_many.side_effect = RuntimeError("delete failed")

        with pytest.raises(WeaviateError, match="Failed to delete"):
            mock_weaviate_service.delete_by_doc_id(project_id=1, doc_id=42)
