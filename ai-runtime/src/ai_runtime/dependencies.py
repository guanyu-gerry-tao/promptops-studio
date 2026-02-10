"""
FastAPI dependency injection setup.

This is the Python equivalent of Spring's @Autowired / @Bean.

In Spring:
    @Bean
    public MilvusService milvusService() { return new MilvusService(settings); }

In FastAPI:
    def get_milvus_service(): return MilvusService(get_settings())

Controllers (routers) declare their dependencies in function parameters,
and FastAPI automatically provides the correct instances.
"""

from functools import lru_cache

from ai_runtime.config import Settings
from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.services.document_service import DocumentService


@lru_cache()
def get_settings() -> Settings:
    """
    Create and cache a single Settings instance.

    @lru_cache() ensures this function only runs once — subsequent calls
    return the same object. This is like a Spring singleton bean.
    """
    return Settings()


@lru_cache()
def get_milvus_service() -> MilvusService:
    """Singleton MilvusService instance."""
    return MilvusService(get_settings())


@lru_cache()
def get_embedding_service() -> EmbeddingService:
    """Singleton EmbeddingService instance."""
    return EmbeddingService(get_settings())


@lru_cache()
def get_document_service() -> DocumentService:
    """Singleton DocumentService — depends on both Milvus and Embedding services."""
    return DocumentService(
        milvus_service=get_milvus_service(),
        embedding_service=get_embedding_service(),
        settings=get_settings(),
    )
