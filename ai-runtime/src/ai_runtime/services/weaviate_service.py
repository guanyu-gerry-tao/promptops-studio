"""
Weaviate vector database service.

Handles hybrid search (vector + BM25 keyword) for the knowledge base.
Weaviate is chosen for its native hybrid search support, aligning with
the DeepLearning.AI RAG course tech stack.

Contrast with MilvusService (pure vector search, frozen):
  - Milvus:   cosine similarity only → good for semantic queries
  - Weaviate: vector + BM25 via alpha parameter → good for mixed queries
              where proper nouns, product names, or IDs matter

Collection naming: same convention as Milvus — one collection per project,
named "Kb{project_id}" (e.g., Kb1, Kb4). Weaviate requires class names
to start with an uppercase letter.
"""

import logging

import weaviate
import weaviate.classes as wvc
from weaviate.classes.query import HybridFusion

from ai_runtime.config import Settings
from ai_runtime.exceptions import WeaviateError

logger = logging.getLogger(__name__)


class WeaviateService:
    def __init__(self, settings: Settings):
        self.settings = settings
        try:
            self.client = weaviate.connect_to_local(
                host=settings.weaviate_host,
                port=settings.weaviate_port,
            )
            logger.info(
                "Connected to Weaviate at %s:%s",
                settings.weaviate_host,
                settings.weaviate_port,
            )
        except Exception as e:
            logger.error("Failed to connect to Weaviate: %s", e)
            raise WeaviateError(
                f"Cannot connect to Weaviate at {settings.weaviate_host}:{settings.weaviate_port}: {e}"
            ) from e

    def _collection_name(self, project_id: int) -> str:
        """Weaviate class names must start with uppercase: Kb1, Kb4, ..."""
        return f"Kb{project_id}"

    def ensure_collection(self, project_id: int):
        """
        Create a Weaviate collection for the project if it doesn't exist.

        Schema (5 properties + 1 vector):
          - doc_id   INT    which document this chunk belongs to (MySQL kb_docs.id)
          - chunk_id INT    chunk index within the document (0, 1, 2, ...)
          - title    TEXT   document title for citations
          - text     TEXT   the actual chunk text (BM25 indexes this automatically)
          - vector   FLOAT[] 1536-dim embedding (stored as the object vector)

        BM25 in Weaviate is automatic — any TEXT property is indexed for
        keyword search with no extra configuration needed.
        """
        name = self._collection_name(project_id)
        try:
            if self.client.collections.exists(name):
                logger.debug("Weaviate collection %s already exists", name)
                return

            logger.info("Creating Weaviate collection: %s", name)
            self.client.collections.create(
                name=name,
                vectorizer_config=wvc.config.Configure.Vectorizer.none(),
                properties=[
                    wvc.config.Property(
                        name="doc_id",
                        data_type=wvc.config.DataType.INT,
                    ),
                    wvc.config.Property(
                        name="chunk_id",
                        data_type=wvc.config.DataType.INT,
                    ),
                    wvc.config.Property(
                        name="title",
                        data_type=wvc.config.DataType.TEXT,
                    ),
                    wvc.config.Property(
                        name="text",
                        data_type=wvc.config.DataType.TEXT,
                    ),
                ],
            )
            logger.info("Weaviate collection %s created", name)

        except WeaviateError:
            raise
        except Exception as e:
            logger.error("Failed to ensure Weaviate collection %s: %s", name, e, exc_info=True)
            raise WeaviateError(f"Failed to create/access Weaviate collection {name}: {e}") from e

    def insert_chunks(
        self,
        project_id: int,
        doc_ids: list[int],
        chunk_ids: list[int],
        titles: list[str],
        texts: list[str],
        embeddings: list[list[float]],
    ) -> int:
        """
        Insert a batch of chunks into the Weaviate collection.

        Each chunk is stored as a Weaviate object with:
          - properties: doc_id, chunk_id, title, text
          - vector: the 1536-dim embedding (passed explicitly since vectorizer=none)

        Uses batch insert for efficiency.

        Returns the number of chunks inserted.
        """
        try:
            self.ensure_collection(project_id)
            name = self._collection_name(project_id)
            collection = self.client.collections.get(name)

            logger.info("Inserting %d chunks into Weaviate project %d", len(doc_ids), project_id)

            with collection.batch.dynamic() as batch:
                for doc_id, chunk_id, title, text, embedding in zip(
                    doc_ids, chunk_ids, titles, texts, embeddings
                ):
                    batch.add_object(
                        properties={
                            "doc_id": doc_id,
                            "chunk_id": chunk_id,
                            "title": title,
                            "text": text,
                        },
                        vector=embedding,
                    )

            logger.info("Weaviate insert complete: %d chunks", len(doc_ids))
            return len(doc_ids)

        except WeaviateError:
            raise
        except Exception as e:
            logger.error(
                "Failed to insert chunks into Weaviate project %d: %s", project_id, e, exc_info=True
            )
            raise WeaviateError(
                f"Failed to insert chunks into Weaviate project {project_id}: {e}"
            ) from e

    def hybrid_search(
        self,
        project_id: int,
        query: str,
        query_embedding: list[float],
        alpha: float,
        top_k: int,
    ) -> list[dict]:
        """
        Hybrid search: combine vector similarity + BM25 keyword search.

        Args:
            project_id:      which project's knowledge base to search
            query:           raw query string (used for BM25 keyword matching)
            query_embedding: vector of the query (used for semantic matching)
            alpha:           blend ratio — 0.0 = pure BM25, 1.0 = pure vector
            top_k:           number of results to return

        Returns:
            List of dicts with: doc_id, chunk_id, title, text, score

        Weaviate's hybrid search runs both paths in parallel and merges
        results using RRF (Reciprocal Rank Fusion) internally.
        """
        name = self._collection_name(project_id)

        if not self.client.collections.exists(name):
            logger.warning(
                "Weaviate collection %s does not exist, returning empty results", name
            )
            return []

        try:
            collection = self.client.collections.get(name)
            logger.info(
                "Weaviate hybrid search: collection=%s, alpha=%.2f, top_k=%d",
                name, alpha, top_k,
            )

            response = collection.query.hybrid(
                query=query,
                vector=query_embedding,
                alpha=alpha,
                limit=top_k,
                fusion_type=HybridFusion.RELATIVE_SCORE,
                return_metadata=wvc.query.MetadataQuery(score=True),
            )

            results = []
            for obj in response.objects:
                results.append({
                    "doc_id": obj.properties.get("doc_id"),
                    "chunk_id": obj.properties.get("chunk_id"),
                    "title": obj.properties.get("title"),
                    "text": obj.properties.get("text"),
                    "score": obj.metadata.score if obj.metadata else 0.0,
                })

            logger.info("Weaviate hybrid search returned %d results", len(results))
            return results

        except Exception as e:
            logger.error(
                "Weaviate hybrid search failed on project %d: %s", project_id, e, exc_info=True
            )
            raise WeaviateError(f"Hybrid search failed on project {project_id}: {e}") from e

    def delete_by_doc_id(self, project_id: int, doc_id: int):
        """Delete all chunks belonging to a specific document."""
        name = self._collection_name(project_id)

        if not self.client.collections.exists(name):
            logger.warning(
                "Weaviate collection %s does not exist, nothing to delete", name
            )
            return

        try:
            collection = self.client.collections.get(name)
            logger.info(
                "Deleting Weaviate chunks with doc_id=%d from %s", doc_id, name
            )
            collection.data.delete_many(
                where=wvc.query.Filter.by_property("doc_id").equal(doc_id)
            )
            logger.info("Weaviate delete complete for doc_id=%d in %s", doc_id, name)

        except Exception as e:
            logger.error(
                "Weaviate delete failed for doc_id=%d in %s: %s", doc_id, name, e, exc_info=True
            )
            raise WeaviateError(
                f"Failed to delete doc_id={doc_id} from Weaviate project {project_id}: {e}"
            ) from e
