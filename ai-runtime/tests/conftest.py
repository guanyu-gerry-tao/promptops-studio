"""
Shared test fixtures for ai-runtime tests.

Fixtures are "pre-made ingredients" that pytest automatically provides to
any test function that asks for them by parameter name.

Why conftest.py?
  - Fixtures defined here are available to ALL test files in this directory
  - No need to import — pytest discovers them automatically
  - Keeps test files focused on test logic, not setup boilerplate
"""

import pytest
from unittest.mock import Mock, MagicMock, patch

from ai_runtime.config import Settings


@pytest.fixture
def fake_settings() -> Settings:
    """
    A Settings object with safe test values.

    Uses real Pydantic validation but with dummy values —
    no .env file needed, no real API keys involved.
    """
    return Settings(
        openai_api_key="test-key-not-real",
        openai_embedding_model="text-embedding-3-small",
        openai_chat_model="gpt-4o-mini",
        milvus_host="localhost",
        milvus_port=19530,
        weaviate_host="localhost",
        weaviate_port=8080,
        weaviate_alpha=0.5,
        rerank_top_k=20,
        rerank_top_n=5,
        chunk_size=500,
        chunk_overlap=50,
        embedding_dimensions=1536,
        retrieve_top_k=5,
    )


@pytest.fixture
def base_settings() -> Settings:
    """
    A mutable Settings object for tests that need to tweak individual fields.
    Unlike fake_settings (which should stay read-only), this is intended to be
    modified by individual test fixtures (e.g., setting rerank_enabled=True).
    """
    return Settings(
        openai_api_key="test-key-not-real",
        openai_embedding_model="text-embedding-3-small",
        openai_chat_model="gpt-4o-mini",
        milvus_host="localhost",
        milvus_port=19530,
        weaviate_host="localhost",
        weaviate_port=8080,
        weaviate_alpha=0.5,
        rerank_top_k=20,
        rerank_top_n=5,
        rerank_enabled=False,
        aws_region="us-east-1",
        bedrock_rerank_model_id="cohere.rerank-v3-5:0",
        chunk_size=500,
        chunk_overlap=50,
        embedding_dimensions=1536,
        retrieve_top_k=5,
    )


@pytest.fixture
def fake_embedding() -> list[float]:
    """
    A fake 1536-dimension embedding vector.

    Real embeddings are 1536 floats from OpenAI, but for tests
    we just need a list of the correct length.
    """
    return [0.1] * 1536


@pytest.fixture
def mock_openai_client(fake_embedding):
    """
    A mock OpenAI client that returns predictable embedding responses.

    Simulates: client.embeddings.create(model=..., input=[...])
    returning a response with .data[i].embedding = fake_embedding
    """
    mock_client = Mock()

    def make_embedding_response(model, input):
        """Build a fake response with one embedding per input text."""
        mock_items = []
        for _ in input:
            item = Mock()
            item.embedding = fake_embedding
            mock_items.append(item)

        response = Mock()
        response.data = mock_items
        return response

    mock_client.embeddings.create.side_effect = make_embedding_response
    return mock_client
