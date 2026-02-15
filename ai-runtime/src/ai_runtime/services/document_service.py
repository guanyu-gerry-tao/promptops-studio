"""
Document processing service.

Orchestrates the full indexing pipeline:
  Markdown text → split into chunks → generate embeddings → store in Milvus

This is the "glue" service that connects EmbeddingService and MilvusService.
"""

import logging

from langchain_text_splitters import RecursiveCharacterTextSplitter

from ai_runtime.config import Settings
from ai_runtime.services.milvus_service import MilvusService
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.exceptions import DocumentProcessingError, AIRuntimeError

logger = logging.getLogger(__name__)


class DocumentService:
    def __init__(
        self,
        milvus_service: MilvusService,
        embedding_service: EmbeddingService,
        settings: Settings,
    ):
        self.milvus = milvus_service
        self.embedding = embedding_service

        # Text splitter: breaks long text into smaller chunks
        # Separators are tried in order — it prefers splitting at headers,
        # then paragraphs, then lines, then words.

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
        Full indexing pipeline for one document.

        Steps:
          1. Split the content into chunks
          2. Generate embedding vectors for all chunks
          3. Store chunks + embeddings in Milvus

        Args:
            project_id: which project this document belongs to
            doc_id: the document's ID in MySQL
            title: document title (stored in Milvus for citations)
            content: full markdown text

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

            # Step 2: Embed (may raise EmbeddingError)
            embeddings = self.embedding.embed_texts(chunks)

            # Step 3: Store in Milvus (may raise MilvusError)
            doc_ids = [doc_id] * len(chunks)
            chunk_ids = list(range(len(chunks)))
            titles = [title] * len(chunks)
            texts = chunks

            self.milvus.insert_chunks(
                project_id=project_id,
                doc_ids=doc_ids,
                chunk_ids=chunk_ids,
                titles=titles,
                texts=texts,
                embeddings=embeddings,
            )

            logger.info("Document processing complete: %d chunks stored", len(chunks))
            return len(chunks)

        except AIRuntimeError:
            # EmbeddingError or MilvusError — already logged, just re-raise
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
        """Remove all chunks for a document from Milvus (for re-indexing)."""
        logger.info("Deleting document: project=%d, doc_id=%d", project_id, doc_id)
        self.milvus.delete_by_doc_id(project_id, doc_id)
