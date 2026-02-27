"""
OpenAI embedding service.

Converts text into numerical vectors (embeddings) using OpenAI's API.
These vectors capture the "meaning" of the text — similar texts produce
similar vectors, which enables semantic search in Milvus.
"""

import logging

import openai

from ai_runtime.config import Settings
from ai_runtime.exceptions import EmbeddingError

logger = logging.getLogger(__name__)


class EmbeddingService:
    def __init__(self, settings: Settings):
        self.client = openai.OpenAI(api_key=settings.openai_api_key)
        self.model = settings.openai_embedding_model

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """
        Convert a list of texts into embedding vectors.

        Args:
            texts: e.g. ["chunk 1 text", "chunk 2 text", "chunk 3 text"]

        Returns:
            A list of vectors, one per text.
            Each vector is a list of 1536 floats.
            e.g. [[0.12, 0.85, ...], [0.34, 0.21, ...], [0.56, 0.78, ...]]
        """
        if not texts:
            return []

        try:
            logger.info("Embedding %d texts with model=%s", len(texts), self.model)
            response = self.client.embeddings.create(
                model=self.model,
                input=texts,
            )
            logger.info("Embedding complete: %d vectors returned", len(response.data))
            return [item.embedding for item in response.data]

        except openai.AuthenticationError as e:
            logger.error("OpenAI authentication failed: %s", e)
            raise EmbeddingError(f"OpenAI API key is invalid or expired: {e}") from e

        except openai.RateLimitError as e:
            logger.warning("OpenAI rate limit hit: %s", e)
            raise EmbeddingError(f"OpenAI rate limit exceeded, try again later: {e}") from e

        except openai.APIError as e:
            logger.error("OpenAI API error: %s", e)
            raise EmbeddingError(f"OpenAI API error: {e}") from e

        except Exception as e:
            logger.error("Unexpected error during embedding: %s", e, exc_info=True)
            raise EmbeddingError(f"Failed to generate embeddings: {e}") from e

    def embed_single(self, text: str) -> list[float]:
        """
        Convert a single text into an embedding vector.
        Convenience wrapper around embed_texts for search queries.
        """
        return self.embed_texts([text])[0]
