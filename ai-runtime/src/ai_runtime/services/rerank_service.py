"""
Reranking service using Amazon Bedrock (Cohere Rerank model).

Why reranking?
  Hybrid search (BM25 + vector) returns candidates ranked by a blend of
  keyword overlap and vector similarity. These are fast approximations.
  A Cross-Encoder reranker scores each (query, chunk) pair together,
  giving a more accurate relevance signal at the cost of extra latency.

Pipeline position:
  Weaviate hybrid search (top_k=20 candidates)
      → RerankService.rerank()
          → Bedrock Cohere Rerank API
      → top_n=5 final results

Flow:
  1. Build the documents list from ChunkResult objects
  2. Call Bedrock InvokeModel with the Cohere Rerank payload
  3. Parse the response → list of {index, relevance_score}
  4. Re-order the original chunks by the returned ranking
  5. Return only the top_n chunks
"""

import json
import logging
import boto3
from botocore.exceptions import BotoCoreError, ClientError

from ai_runtime.config import Settings
from ai_runtime.exceptions import RerankError

logger = logging.getLogger(__name__)


class RerankService:
    """
    Wraps Amazon Bedrock Cohere Rerank API.

    boto3 reads AWS credentials automatically from ~/.aws/credentials
    (configured via `aws configure`), so no explicit key passing is needed.
    """

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        try:
            self._client = boto3.client(
                "bedrock-runtime",
                region_name=settings.aws_region,
            )
        except Exception as e:
            raise RerankError(f"Failed to initialize Bedrock client: {e}") from e

    def rerank(self, query: str, chunks: list[dict], top_n: int) -> list[dict]:
        """
        Rerank chunks by relevance to the query using Cohere Rerank on Bedrock.

        Args:
            query:   The user's search query string.
            chunks:  List of chunk dicts, each must have at least a 'text' key.
                     (These come directly from WeaviateService.hybrid_search output.)
            top_n:   How many top results to return after reranking.

        Returns:
            A list of chunk dicts, reordered by relevance, length = min(top_n, len(chunks)).
            Each chunk dict gets an extra 'score' field updated with the rerank score.

        Raises:
            RerankError: on any Bedrock API failure.
        """
        if not chunks:
            logger.warning("rerank() called with empty chunks list, returning empty")
            return []

        # Cohere Rerank expects plain text strings, not dicts
        documents = [chunk["text"] for chunk in chunks]

        payload = {
            "query": query,
            "documents": documents,
            "top_n": top_n,
            "api_version": 2,
        }

        logger.info(
            "Calling Bedrock Cohere Rerank: model=%s, docs=%d, top_n=%d",
            self._settings.bedrock_rerank_model_id,
            len(documents),
            top_n,
        )

        try:
            response = self._client.invoke_model(
                modelId=self._settings.bedrock_rerank_model_id,
                body=json.dumps(payload),
                contentType="application/json",
                accept="application/json",
            )
        except (BotoCoreError, ClientError) as e:
            raise RerankError(f"Bedrock Rerank API call failed: {e}") from e

        # Parse the response body
        try:
            body = json.loads(response["body"].read())
            # body["results"] is a list of {"index": int, "relevance_score": float}
            rerank_results = body["results"]
        except (KeyError, json.JSONDecodeError) as e:
            raise RerankError(f"Failed to parse Bedrock Rerank response: {e}") from e

        # Re-order the original chunks according to the rerank ranking,
        # and update the score field with the rerank relevance score
        reranked = []
        for result in rerank_results:
            original_chunk = dict(chunks[result["index"]])   # copy to avoid mutating input
            original_chunk["score"] = result["relevance_score"]
            reranked.append(original_chunk)

        logger.info("Reranking complete: %d → %d chunks", len(chunks), len(reranked))
        return reranked
