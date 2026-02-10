"""
OpenAI embedding service.

Converts text into numerical vectors (embeddings) using OpenAI's API.
These vectors capture the "meaning" of the text — similar texts produce
similar vectors, which enables semantic search in Milvus.
"""

import openai

from ai_runtime.config import Settings


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
        # TODO(human): Call the OpenAI embeddings API
        # Hints:
        #   1. Use self.client.embeddings.create(model=self.model, input=texts)
        #   2. The response has a .data attribute, which is a list of objects
        #   3. Each object has an .embedding attribute (the vector)
        #   4. Return the list of vectors in the same order as the input texts
        #   5. Tip: response.data is already sorted by index, so you can do:
        #      return [item.embedding for item in response.data]
        raise NotImplementedError("TODO: implement embed_texts")

    def embed_single(self, text: str) -> list[float]:
        """
        Convert a single text into an embedding vector.
        Convenience wrapper around embed_texts for search queries.
        """
        return self.embed_texts([text])[0]
