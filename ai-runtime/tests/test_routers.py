"""
Integration tests for API routers (index + retrieve).

Uses FastAPI's TestClient to send real HTTP requests to the app,
but with mocked service dependencies — so we test the full HTTP layer
(validation, serialization, status codes, exception handlers) without
needing real OpenAI/Milvus/Weaviate connections.

This is the Python equivalent of Spring's @WebMvcTest + @MockBean.
"""

import pytest
from unittest.mock import Mock, patch
from fastapi.testclient import TestClient

from ai_runtime.main import app
from ai_runtime.dependencies import (
    get_document_service,
    get_milvus_service,
    get_weaviate_service,
    get_embedding_service,
    get_rerank_service,
    get_settings,
)
from ai_runtime.config import Settings
from ai_runtime.exceptions import EmbeddingError, MilvusError, WeaviateError


# ──────────────────────────────────────
# Fixtures: override FastAPI dependencies
# ──────────────────────────────────────

@pytest.fixture
def mock_doc_service():
    return Mock()


@pytest.fixture
def mock_milvus_svc():
    return Mock()


@pytest.fixture
def mock_weaviate_svc():
    return Mock()


@pytest.fixture
def mock_embedding_svc():
    return Mock()


@pytest.fixture
def mock_rerank_svc():
    """Mock RerankService — returns chunks unchanged (pass-through) by default."""
    svc = Mock()
    # Default: rerank() returns whatever chunks it receives (no-op)
    svc.rerank.side_effect = lambda query, chunks, top_n: chunks[:top_n]
    return svc


@pytest.fixture
def client(fake_settings, mock_doc_service, mock_milvus_svc,
           mock_weaviate_svc, mock_embedding_svc, mock_rerank_svc):
    """
    FastAPI TestClient with all service dependencies replaced by mocks.

    app.dependency_overrides is FastAPI's way to inject mock dependencies —
    same concept as Spring's @MockBean but done via a dictionary.
    """
    app.dependency_overrides[get_settings] = lambda: fake_settings
    app.dependency_overrides[get_document_service] = lambda: mock_doc_service
    app.dependency_overrides[get_milvus_service] = lambda: mock_milvus_svc
    app.dependency_overrides[get_weaviate_service] = lambda: mock_weaviate_svc
    app.dependency_overrides[get_embedding_service] = lambda: mock_embedding_svc
    app.dependency_overrides[get_rerank_service] = lambda: mock_rerank_svc

    with TestClient(app) as c:
        yield c

    app.dependency_overrides.clear()


# ──────────────────────────────────────
# POST /index-document
# ──────────────────────────────────────

class TestIndexEndpoint:
    """Tests for POST /index-document."""

    def test_success_returns_200(self, client, mock_doc_service):
        """Happy path: document indexed successfully."""
        mock_doc_service.process_document.return_value = 3  # 3 chunks

        response = client.post("/index-document", json={
            "project_id": 1,
            "doc_id": 10,
            "title": "Test Doc",
            "content": "Hello world content",
        })

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "SUCCESS"
        assert data["chunks_count"] == 3
        assert data["doc_id"] == 10

    def test_validation_error_returns_422(self, client):
        """Missing required fields → 422 (FastAPI auto-validation)."""
        response = client.post("/index-document", json={
            "project_id": 1,
            # missing doc_id, title, content
        })

        assert response.status_code == 422

    def test_embedding_error_returns_502(self, client, mock_doc_service):
        """EmbeddingError → global handler → 502."""
        mock_doc_service.process_document.side_effect = EmbeddingError("OpenAI is down")

        response = client.post("/index-document", json={
            "project_id": 1, "doc_id": 10, "title": "Test", "content": "text",
        })

        assert response.status_code == 502
        assert response.json()["error"] == "embedding_error"

    def test_milvus_error_returns_502(self, client, mock_doc_service):
        """MilvusError → global handler → 502."""
        mock_doc_service.process_document.side_effect = MilvusError("Milvus unreachable")

        response = client.post("/index-document", json={
            "project_id": 1, "doc_id": 10, "title": "Test", "content": "text",
        })

        assert response.status_code == 502
        assert response.json()["error"] == "milvus_error"


# ──────────────────────────────────────
# POST /retrieve-document
# ──────────────────────────────────────

FAKE_CHUNKS = [
    {"doc_id": 10, "chunk_id": 0, "title": "Doc A", "text": "hello", "score": 0.9},
]

class TestRetrieveEndpoint:
    """Tests for POST /retrieve-document."""

    def test_no_alpha_uses_weaviate_with_default_alpha(self, client, mock_embedding_svc, mock_weaviate_svc):
        """alpha=None (default) → routes to Weaviate using settings.weaviate_alpha (0.5)."""
        mock_embedding_svc.embed_single.return_value = [0.1] * 1536
        mock_weaviate_svc.hybrid_search.return_value = FAKE_CHUNKS

        response = client.post("/retrieve-document", json={
            "project_id": 1,
            "query": "test query",
            "top_k": 3,
            "generate_answer": False,
        })

        assert response.status_code == 200
        assert len(response.json()["results"]) == 1
        mock_weaviate_svc.hybrid_search.assert_called_once()
        call_kwargs = mock_weaviate_svc.hybrid_search.call_args[1]
        assert call_kwargs["alpha"] == 0.5  # settings.weaviate_alpha default

    def test_alpha_uses_weaviate_hybrid(self, client, mock_embedding_svc, mock_weaviate_svc):
        """alpha provided → routes to Weaviate hybrid search, Milvus not called."""
        mock_embedding_svc.embed_single.return_value = [0.1] * 1536
        mock_weaviate_svc.hybrid_search.return_value = FAKE_CHUNKS

        response = client.post("/retrieve-document", json={
            "project_id": 1,
            "query": "test query",
            "top_k": 3,
            "generate_answer": False,
            "alpha": 0.5,
        })

        assert response.status_code == 200
        assert len(response.json()["results"]) == 1
        mock_weaviate_svc.hybrid_search.assert_called_once()

    def test_alpha_zero_pure_keyword(self, client, mock_embedding_svc, mock_weaviate_svc):
        """alpha=0.0 → pure BM25 keyword search via Weaviate."""
        mock_embedding_svc.embed_single.return_value = [0.1] * 1536
        mock_weaviate_svc.hybrid_search.return_value = FAKE_CHUNKS

        response = client.post("/retrieve-document", json={
            "project_id": 1,
            "query": "test query",
            "generate_answer": False,
            "alpha": 0.0,
        })

        assert response.status_code == 200
        call_kwargs = mock_weaviate_svc.hybrid_search.call_args[1]
        assert call_kwargs["alpha"] == 0.0

    def test_search_without_answer(self, client, mock_embedding_svc, mock_weaviate_svc):
        """generate_answer=False: returns chunks, answer is None."""
        mock_embedding_svc.embed_single.return_value = [0.1] * 1536
        mock_weaviate_svc.hybrid_search.return_value = FAKE_CHUNKS

        response = client.post("/retrieve-document", json={
            "project_id": 1,
            "query": "test query",
            "top_k": 3,
            "generate_answer": False,
        })

        assert response.status_code == 200
        data = response.json()
        assert data["results"][0]["score"] == 0.9
        assert data["answer"] is None

    def test_validation_error_missing_query(self, client):
        """Missing 'query' field → 422."""
        response = client.post("/retrieve-document", json={"project_id": 1})
        assert response.status_code == 422

    def test_embedding_error_returns_502(self, client, mock_embedding_svc):
        """EmbeddingError during query embedding → 502."""
        mock_embedding_svc.embed_single.side_effect = EmbeddingError("API key expired")

        response = client.post("/retrieve-document", json={
            "project_id": 1, "query": "test",
        })

        assert response.status_code == 502
        assert response.json()["error"] == "embedding_error"


# ──────────────────────────────────────
# Built-in endpoints
# ──────────────────────────────────────

class TestBuiltinEndpoints:
    """Tests for health check and root endpoint."""

    def test_health_check(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "OK"

    def test_root(self, client):
        response = client.get("/")
        assert response.status_code == 200
        assert "AI Runtime Service" in response.json()["message"]
