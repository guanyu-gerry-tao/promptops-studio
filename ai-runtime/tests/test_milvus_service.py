"""
Unit tests for MilvusService.

Strategy: Mock all pymilvus objects (connections, Collection, utility)
so we never need a real Milvus server running.
"""

import pytest
from unittest.mock import patch, Mock, MagicMock, call

from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.exceptions import MilvusError


# ──────────────────────────────────────
# Helper: create a MilvusService with mocked pymilvus
# ──────────────────────────────────────

@pytest.fixture
def milvus_service(fake_settings):
    """
    A MilvusService instance where pymilvus.connections.connect is mocked.
    This avoids needing a real Milvus server.
    """
    with patch("ai_runtime.services.milvus_service.connections") as mock_conn:
        service = MilvusService(fake_settings)
        mock_conn.connect.assert_called_once_with(
            alias="default",
            host="localhost",
            port=19530,
        )
    return service


class TestInit:
    """Tests for MilvusService.__init__()."""

    def test_connection_failure_raises_milvus_error(self, fake_settings):
        """If Milvus is unreachable, constructor raises MilvusError."""
        with patch("ai_runtime.services.milvus_service.connections") as mock_conn:
            mock_conn.connect.side_effect = ConnectionError("refused")

            with pytest.raises(MilvusError, match="Cannot connect"):
                MilvusService(fake_settings)


class TestEnsureCollection:
    """Tests for MilvusService.ensure_collection()."""

    def test_returns_existing_collection(self, milvus_service):
        """If collection already exists, just return it (no creation)."""
        mock_collection = Mock()

        with (
            patch("ai_runtime.services.milvus_service.utility") as mock_util,
            patch("ai_runtime.services.milvus_service.Collection", return_value=mock_collection),
        ):
            mock_util.has_collection.return_value = True
            result = milvus_service.ensure_collection(project_id=1)

        assert result == mock_collection
        mock_util.has_collection.assert_called_once_with("kb_1")

    def test_creates_new_collection_with_index(self, milvus_service):
        """If collection doesn't exist, create it with IVF_FLAT index."""
        mock_collection = Mock()

        with (
            patch("ai_runtime.services.milvus_service.utility") as mock_util,
            patch("ai_runtime.services.milvus_service.Collection", return_value=mock_collection),
            patch("ai_runtime.services.milvus_service.CollectionSchema"),
        ):
            mock_util.has_collection.return_value = False
            result = milvus_service.ensure_collection(project_id=42)

        assert result == mock_collection
        # Verify index was created on the "embedding" field
        mock_collection.create_index.assert_called_once()
        index_args = mock_collection.create_index.call_args
        assert index_args[0][0] == "embedding"
        assert index_args[0][1]["index_type"] == "IVF_FLAT"
        assert index_args[0][1]["metric_type"] == "COSINE"

    def test_wraps_unexpected_error_as_milvus_error(self, milvus_service):
        """Non-MilvusError exceptions get wrapped."""
        with patch("ai_runtime.services.milvus_service.utility") as mock_util:
            mock_util.has_collection.side_effect = RuntimeError("disk full")

            with pytest.raises(MilvusError, match="Failed to create/access"):
                milvus_service.ensure_collection(project_id=1)


class TestInsertChunks:
    """Tests for MilvusService.insert_chunks()."""

    def test_inserts_and_flushes(self, milvus_service):
        """Happy path: insert 2 chunks, flush, return count."""
        mock_collection = Mock()

        with patch.object(milvus_service, "ensure_collection", return_value=mock_collection):
            result = milvus_service.insert_chunks(
                project_id=1,
                doc_ids=[10, 10],
                chunk_ids=[0, 1],
                titles=["Doc A", "Doc A"],
                texts=["chunk 0", "chunk 1"],
                embeddings=[[0.1] * 1536, [0.2] * 1536],
            )

        assert result == 2
        mock_collection.insert.assert_called_once()
        mock_collection.flush.assert_called_once()

    def test_wraps_insert_error_as_milvus_error(self, milvus_service):
        """If insert fails, wrap as MilvusError."""
        mock_collection = Mock()
        mock_collection.insert.side_effect = RuntimeError("write failed")

        with patch.object(milvus_service, "ensure_collection", return_value=mock_collection):
            with pytest.raises(MilvusError, match="Failed to insert"):
                milvus_service.insert_chunks(
                    project_id=1,
                    doc_ids=[10],
                    chunk_ids=[0],
                    titles=["Doc"],
                    texts=["text"],
                    embeddings=[[0.1] * 1536],
                )


class TestSearch:
    """Tests for MilvusService.search()."""

    def test_returns_empty_for_nonexistent_collection(self, milvus_service):
        """If the project has no collection yet, return empty list."""
        with patch("ai_runtime.services.milvus_service.utility") as mock_util:
            mock_util.has_collection.return_value = False
            result = milvus_service.search(project_id=999, query_embedding=[0.1] * 1536)

        assert result == []

    def test_returns_formatted_hits(self, milvus_service):
        """Happy path: search returns hits formatted as dicts."""
        # Build fake Milvus search results
        hit1 = Mock()
        hit1.entity.get.side_effect = lambda k: {"doc_id": 10, "chunk_id": 0, "title": "Doc A", "text": "hello"}[k]
        hit1.score = 0.95

        hit2 = Mock()
        hit2.entity.get.side_effect = lambda k: {"doc_id": 10, "chunk_id": 1, "title": "Doc A", "text": "world"}[k]
        hit2.score = 0.82

        mock_collection = Mock()
        mock_collection.search.return_value = [[hit1, hit2]]

        with (
            patch("ai_runtime.services.milvus_service.utility") as mock_util,
            patch("ai_runtime.services.milvus_service.Collection", return_value=mock_collection),
        ):
            mock_util.has_collection.return_value = True
            result = milvus_service.search(
                project_id=1,
                query_embedding=[0.1] * 1536,
                top_k=2,
            )

        assert len(result) == 2
        assert result[0]["doc_id"] == 10
        assert result[0]["score"] == 0.95
        assert result[1]["text"] == "world"
        mock_collection.load.assert_called_once()


class TestDeleteByDocId:
    """Tests for MilvusService.delete_by_doc_id()."""

    def test_deletes_and_flushes(self, milvus_service):
        """Happy path: delete chunks by doc_id."""
        mock_collection = Mock()

        with (
            patch("ai_runtime.services.milvus_service.utility") as mock_util,
            patch("ai_runtime.services.milvus_service.Collection", return_value=mock_collection),
        ):
            mock_util.has_collection.return_value = True
            milvus_service.delete_by_doc_id(project_id=1, doc_id=10)

        mock_collection.delete.assert_called_once_with(expr="doc_id == 10")
        mock_collection.flush.assert_called_once()

    def test_skips_if_collection_missing(self, milvus_service):
        """No collection → no error, just skip."""
        with patch("ai_runtime.services.milvus_service.utility") as mock_util:
            mock_util.has_collection.return_value = False
            # Should not raise
            milvus_service.delete_by_doc_id(project_id=999, doc_id=10)
