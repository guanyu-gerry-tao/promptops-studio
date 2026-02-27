"""
Unit tests for EmbeddingService.

Strategy: Mock the OpenAI client so we never call the real API.
We're testing OUR logic (error wrapping, empty input handling, etc.),
not OpenAI's servers.
"""

import openai
import pytest
from unittest.mock import patch, Mock

from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.exceptions import EmbeddingError


class TestEmbedTexts:
    """Tests for EmbeddingService.embed_texts()."""

    def test_returns_embeddings_for_multiple_texts(self, fake_settings, mock_openai_client):
        """Happy path: 3 texts in → 3 embedding vectors out."""
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        texts = ["hello", "world", "test"]
        result = service.embed_texts(texts)

        assert len(result) == 3
        assert all(len(vec) == 1536 for vec in result)
        mock_openai_client.embeddings.create.assert_called_once_with(
            model="text-embedding-3-small",
            input=texts,
        )

    def test_returns_empty_list_for_empty_input(self, fake_settings, mock_openai_client):
        """Edge case: empty list → empty list, no API call."""
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        result = service.embed_texts([])

        assert result == []
        mock_openai_client.embeddings.create.assert_not_called()

    def test_wraps_auth_error_as_embedding_error(self, fake_settings, mock_openai_client):
        """AuthenticationError (bad API key) → EmbeddingError."""
        mock_openai_client.embeddings.create.side_effect = openai.AuthenticationError(
            message="Invalid API key",
            response=Mock(status_code=401),
            body=None,
        )
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        with pytest.raises(EmbeddingError, match="invalid or expired"):
            service.embed_texts(["test"])

    def test_wraps_rate_limit_error_as_embedding_error(self, fake_settings, mock_openai_client):
        """RateLimitError (too many requests) → EmbeddingError."""
        mock_openai_client.embeddings.create.side_effect = openai.RateLimitError(
            message="Rate limit exceeded",
            response=Mock(status_code=429),
            body=None,
        )
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        with pytest.raises(EmbeddingError, match="rate limit"):
            service.embed_texts(["test"])

    def test_wraps_api_error_as_embedding_error(self, fake_settings, mock_openai_client):
        """Generic OpenAI APIError → EmbeddingError."""
        mock_openai_client.embeddings.create.side_effect = openai.APIError(
            message="Server error",
            request=Mock(),
            body=None,
        )
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        with pytest.raises(EmbeddingError, match="API error"):
            service.embed_texts(["test"])

    def test_wraps_unexpected_error_as_embedding_error(self, fake_settings, mock_openai_client):
        """Any other exception → EmbeddingError (catch-all)."""
        mock_openai_client.embeddings.create.side_effect = RuntimeError("network down")
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        with pytest.raises(EmbeddingError, match="Failed to generate embeddings"):
            service.embed_texts(["test"])


class TestEmbedSingle:
    """Tests for EmbeddingService.embed_single()."""

    def test_returns_single_vector(self, fake_settings, mock_openai_client):
        """embed_single wraps embed_texts and returns the first vector."""
        with patch("ai_runtime.services.embedding_service.openai.OpenAI", return_value=mock_openai_client):
            service = EmbeddingService(fake_settings)

        result = service.embed_single("hello")

        assert len(result) == 1536
        # Verify it passed a single-element list to the API
        mock_openai_client.embeddings.create.assert_called_once_with(
            model="text-embedding-3-small",
            input=["hello"],
        )
