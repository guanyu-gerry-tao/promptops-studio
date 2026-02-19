"""
Application configuration.

Uses pydantic-settings to load environment variables from .env file.
This is the Python equivalent of Spring Boot's application.yml + @Value annotation.
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    All configuration values for the AI Runtime service.

    How it works:
    - Each field name maps to an environment variable (case-insensitive).
    - For example, `openai_api_key` reads from `OPENAI_API_KEY` in .env.
    - Fields with default values (like `milvus_host`) are optional in .env.
    - Fields without defaults (like `openai_api_key`) are required — the app
      will fail to start if they're missing.
    """

    # --- OpenAI ---
    openai_api_key: str                                  # Required: no default
    openai_embedding_model: str = "text-embedding-3-small"  # 1536 dimensions, cheapest
    openai_chat_model: str = "gpt-4o-mini"               # For generating answers in /retrieve

    # --- Milvus (pure vector search, frozen) ---
    milvus_host: str = "localhost"
    milvus_port: int = 19530

    # --- Weaviate (hybrid search: vector + BM25) ---
    weaviate_host: str = "localhost"
    weaviate_port: int = 8080
    weaviate_alpha: float = 0.5  # 0.0 = pure BM25, 1.0 = pure vector, 0.5 = balanced hybrid
    rerank_top_k: int = 20       # candidates to fetch before reranking
    rerank_top_n: int = 5        # results to keep after reranking (≤ rerank_top_k)

    # --- Reranking (Amazon Bedrock, Cohere Rerank model) ---
    # Set RERANK_ENABLED=true in .env to activate.
    # AWS credentials are read from ~/.aws/credentials automatically by boto3;
    # no need to set AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY in .env
    # unless you want to override the default profile.
    rerank_enabled: bool = False
    aws_region: str = "us-east-1"
    bedrock_rerank_model_id: str = "cohere.rerank-v3-5:0"

    # --- Document processing ---
    chunk_size: int = 500        # Max characters per chunk
    chunk_overlap: int = 50      # Overlap between consecutive chunks
    embedding_dimensions: int = 1536  # Must match the embedding model's output
    retrieve_top_k: int = 5      # Default number of search results (Milvus)

    model_config = {
        "env_file": ".env",      # Load variables from this file
    }
