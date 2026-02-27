"""
Document processing service.

Orchestrates the full indexing pipeline:
  Markdown text → split into chunks → generate embeddings → store in Weaviate

# MILVUS (dead code — kept for rollback):
# MilvusService parameter is still accepted in __init__ and stored as self.milvus,
# but insert_chunks and delete_by_doc_id are no longer called.
# To re-enable: uncomment the Milvus blocks in process_document() and delete_document().
"""

import logging

from langchain_text_splitters import RecursiveCharacterTextSplitter

from ai_runtime.config import Settings
from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.services.weaviate_service import WeaviateService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.exceptions import DocumentProcessingError, AIRuntimeError

logger = logging.getLogger(__name__)


class DocumentService:
    def __init__(
        self,
        milvus_service: MilvusService | None,   # None = Milvus phased out
        weaviate_service: WeaviateService,
        embedding_service: EmbeddingService,
        settings: Settings,
    ):
        self.milvus = milvus_service   # dead code — kept for rollback, currently None
        self.weaviate = weaviate_service
        self.embedding = embedding_service

        # NOTE:
        # separators define where to split the document. starting from ##: markdown second headline.
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.chunk_size,
            chunk_overlap=settings.chunk_overlap,
            separators=["\n## ", "\n### ", "\n\n", "\n", " ", ""],
        )

    def process_document(
        self,
        project_id: int,
        doc_id: int,
        title: str,
        content: str,
    ) -> int:
        """
        Full indexing pipeline for one document. Dual-writes to Milvus + Weaviate.

        Steps:
          1. Split the content into chunks
          2. Generate embedding vectors for all chunks (one API call, shared)
          3. Store chunks + embeddings in Milvus (pure vector, frozen)
          4. Store chunks + embeddings in Weaviate (hybrid search, new)

        Returns:
            Number of chunks created and stored.
        """
        logger.info(
            "Processing document: project=%d, doc_id=%d, title='%s', content_length=%d",
            project_id, doc_id, title, len(content),
        )

        try:
            # Step 1: Split
            chunks = self.splitter.split_text(content)
            if not chunks:
                logger.warning("No chunks produced for doc_id=%d (content may be empty)", doc_id)
                return 0
            logger.info("Split into %d chunks", len(chunks))

            # Step 2: Embed
            embeddings = self.embedding.embed_texts(chunks)

            doc_ids = [doc_id] * len(chunks)
            chunk_ids = list(range(len(chunks)))
            titles = [title] * len(chunks)

            # MILVUS (dead code — kept for rollback):
            # self.milvus.insert_chunks(
            #     project_id=project_id, doc_ids=doc_ids, chunk_ids=chunk_ids,
            #     titles=titles, texts=chunks, embeddings=embeddings,
            # )
            # logger.info("Milvus insert complete: %d chunks", len(chunks))

            # Step 3: Store in Weaviate (hybrid search)
            self.weaviate.insert_chunks(
                project_id=project_id,
                doc_ids=doc_ids,
                chunk_ids=chunk_ids,
                titles=titles,
                texts=chunks,
                embeddings=embeddings,
            )
            logger.info("Weaviate insert complete: %d chunks", len(chunks))

            logger.info("Document processing complete: %d chunks stored", len(chunks))
            return len(chunks)

        except AIRuntimeError:
            # EmbeddingError, MilvusError, WeaviateError — already logged, just re-raise
            raise

        except Exception as e:
            logger.error(
                "Unexpected error processing doc_id=%d in project %d: %s",
                doc_id, project_id, e, exc_info=True,
            )
            raise DocumentProcessingError(
                f"Failed to process document {doc_id} in project {project_id}: {e}"
            ) from e

    def delete_document(self, project_id: int, doc_id: int):
        """Remove all chunks for a document from Weaviate."""
        logger.info("Deleting document: project=%d, doc_id=%d", project_id, doc_id)
        # MILVUS (dead code — kept for rollback):
        # self.milvus.delete_by_doc_id(project_id, doc_id)
        self.weaviate.delete_by_doc_id(project_id, doc_id)
