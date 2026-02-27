"""
Unit tests for RerankService.

Strategy: mock boto3.client so we never make real AWS calls.
The tests verify:
  - Happy path: correct reordering + score update
  - Empty input: returns [] immediately, no API call
  - API failure: BotoCoreError → RerankError
  - Bad response format: missing 'results' key → RerankError
"""

import json
import pytest
from io import BytesIO
from unittest.mock import MagicMock, patch

from ai_runtime.services.rerank_service import RerankService
from ai_runtime.exceptions import RerankError


# ── Fixtures ────────────────────────────────────────────────────────────────

@pytest.fixture
def settings(base_settings):
    """Settings with reranking enabled and a test model id."""
    base_settings.rerank_enabled = True
    base_settings.aws_region = "us-east-1"
    base_settings.bedrock_rerank_model_id = "cohere.rerank-v3-5:0"
    return base_settings


@pytest.fixture
def mock_bedrock_client():
    """A mock boto3 bedrock-runtime client."""
    return MagicMock()


@pytest.fixture
def rerank_svc(settings, mock_bedrock_client):
    """RerankService with the boto3 client swapped for a mock."""
    with patch("ai_runtime.services.rerank_service.boto3.client", return_value=mock_bedrock_client):
        svc = RerankService(settings)
    return svc, mock_bedrock_client


# ── Helpers ─────────────────────────────────────────────────────────────────

def make_bedrock_response(results: list[dict]) -> dict:
    """
    Build a fake Bedrock invoke_model response.
    The real SDK wraps the body in a StreamingBody — we simulate that with BytesIO.
    """
    body_bytes = json.dumps({"results": results}).encode()
    return {"body": BytesIO(body_bytes)}


def make_chunks(texts: list[str]) -> list[dict]:
    """Build minimal chunk dicts as returned by WeaviateService.hybrid_search."""
    return [
        {"doc_id": i, "chunk_id": i, "text": t, "score": 0.5, "title": f"Doc {i}"}
        for i, t in enumerate(texts)
    ]


# ── Tests ────────────────────────────────────────────────────────────────────

class TestRerank:
    def test_happy_path_reorders_and_updates_scores(self, rerank_svc):
        """
        Cohere returns index=[1,0] → chunk at position 1 should come first.
        Scores should be updated to the rerank relevance scores.
        """
        svc, mock_client = rerank_svc
        chunks = make_chunks(["chunk A", "chunk B"])

        # Cohere ranks chunk B (index 1) higher than chunk A (index 0)
        mock_client.invoke_model.return_value = make_bedrock_response([
            {"index": 1, "relevance_score": 0.95},
            {"index": 0, "relevance_score": 0.42},
        ])

        result = svc.rerank(query="test query", chunks=chunks, top_n=2)

        assert len(result) == 2
        assert result[0]["text"] == "chunk B"   # highest relevance first
        assert result[0]["score"] == 0.95
        assert result[1]["text"] == "chunk A"
        assert result[1]["score"] == 0.42

    def test_does_not_mutate_original_chunks(self, rerank_svc):
        """rerank() should return new dicts, not modify the input list."""
        svc, mock_client = rerank_svc
        chunks = make_chunks(["only chunk"])
        original_score = chunks[0]["score"]

        mock_client.invoke_model.return_value = make_bedrock_response([
            {"index": 0, "relevance_score": 0.99},
        ])

        svc.rerank(query="q", chunks=chunks, top_n=1)

        # original dict must be untouched
        assert chunks[0]["score"] == original_score

    def test_empty_chunks_returns_empty_without_api_call(self, rerank_svc):
        """If chunks is empty, skip the API call and return []."""
        svc, mock_client = rerank_svc

        result = svc.rerank(query="q", chunks=[], top_n=5)

        assert result == []
        mock_client.invoke_model.assert_not_called()

    def test_api_failure_raises_rerank_error(self, rerank_svc):
        """BotoCoreError from invoke_model → RerankError."""
        from botocore.exceptions import BotoCoreError

        svc, mock_client = rerank_svc
        mock_client.invoke_model.side_effect = BotoCoreError()

        with pytest.raises(RerankError):
            svc.rerank(query="q", chunks=make_chunks(["x"]), top_n=1)

    def test_bad_response_format_raises_rerank_error(self, rerank_svc):
        """If Bedrock response body is missing 'results' key → RerankError."""
        svc, mock_client = rerank_svc
        # Return a body without the expected 'results' key
        body_bytes = json.dumps({"unexpected_key": []}).encode()
        mock_client.invoke_model.return_value = {"body": BytesIO(body_bytes)}

        with pytest.raises(RerankError):
            svc.rerank(query="q", chunks=make_chunks(["x"]), top_n=1)

    def test_boto3_client_init_failure_raises_rerank_error(self, settings):
        """If boto3.client raises during __init__ → RerankError."""
        with patch("ai_runtime.services.rerank_service.boto3.client", side_effect=Exception("no credentials")):
            with pytest.raises(RerankError):
                RerankService(settings)
