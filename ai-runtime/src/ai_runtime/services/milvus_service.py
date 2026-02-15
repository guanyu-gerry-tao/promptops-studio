"""
Milvus vector database service.

Handles:
  - Creating collections (one per project: kb_1, kb_2, ...)
  - Inserting document chunks with their embedding vectors
  - Searching for similar vectors (semantic search)
  - Deleting chunks when a document is re-indexed
"""

from pymilvus import (
  connections,
  Collection,
  CollectionSchema,
  FieldSchema,
  DataType,
  utility,
)

from ai_runtime.config import Settings


class MilvusService:
  def __init__(self, settings: Settings):
    self.settings = settings
    # Connect to Milvus server on startup
    connections.connect(
        alias="default",
        host=settings.milvus_host,
        port=settings.milvus_port,
    )

  def _collection_name(self, project_id: int) -> str:
    """Generate collection name for a project: kb_1, kb_2, etc."""
    return f"kb_{project_id}"

  def ensure_collection(self, project_id: int) -> Collection:
    """
    Create a Milvus collection for the project if it doesn't exist.
    If it already exists, just return it.

    Collection schema (6 fields):
      - id:        INT64, primary key, auto-generated
      - doc_id:    INT64, which document this chunk belongs to
      - chunk_id:  INT64, chunk index within the document (0, 1, 2, ...)
      - title:     VARCHAR(512), document title for citations
      - text:      VARCHAR(8192), the actual chunk text
      - embedding: FLOAT_VECTOR(1536), the embedding vector

    Index: IVF_FLAT with COSINE metric on the embedding field.
    """
    name = self._collection_name(project_id)

    # If collection already exists, return it
    if utility.has_collection(name):
      return Collection(name)

    fields = [
      FieldSchema(name="id", dtype=DataType.INT64, is_primary=True,
                  auto_id=True),
      FieldSchema(name="doc_id", dtype=DataType.INT64),
      FieldSchema(name="chunk_id", dtype=DataType.INT64),
      FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=512),
      FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=8192),
      FieldSchema(
          name="embedding",
          dtype=DataType.FLOAT_VECTOR,
          dim=self.settings.embedding_dimensions,
      ),
    ]

    schema = CollectionSchema(fields,
                              description=f"Milvus knowledge base for project {project_id}")

    collection = Collection(name, schema)

    collection.create_index(
        "embedding",
        {"index_type": "IVF_FLAT",
         "metric_type": "COSINE",
         "params": {"nlist": 128}
         }
    )

    return collection

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
    Insert a batch of chunks into the Milvus collection.

    Each parameter is a list of the same length. For example, if we have
    3 chunks, then doc_ids has 3 elements, chunk_ids has 3, etc.

    Returns the number of chunks inserted.
    """
    collection = self.ensure_collection(project_id)
    collection.insert([doc_ids, chunk_ids, titles, texts, embeddings])
    collection.flush()
    return len(doc_ids)

  def search(
      self,
      project_id: int,
      query_embedding: list[float],
      top_k: int = 5,
  ) -> list[dict]:
    """
    Search for the most similar chunks in the collection.

    Args:
        project_id: which project's knowledge base to search
        query_embedding: the embedding vector of the user's query
        top_k: how many results to return

    Returns:
        A list of dicts, each with: doc_id, chunk_id, title, text, score
    """
    name = self._collection_name(project_id)

    if not utility.has_collection(name):
      return []

    collection = Collection(name)
    collection.load()

    results = collection.search(
        data=[query_embedding],
        anns_field="embedding",
        param={"metric_type": "COSINE", "params": {"nprobe": 16}},
        limit=top_k,
        output_fields=["doc_id", "chunk_id", "title", "text"],
    )

    hits = results[0]  # results for our single query
    return [
        {
            "doc_id": hit.entity.get("doc_id"),
            "chunk_id": hit.entity.get("chunk_id"),
            "title": hit.entity.get("title"),
            "text": hit.entity.get("text"),
            "score": hit.score,
        }
        for hit in hits
    ]

  def delete_by_doc_id(self, project_id: int, doc_id: int):
    """Delete all chunks belonging to a specific document."""
    name = self._collection_name(project_id)

    if not utility.has_collection(name):
      return

    collection = Collection(name)
    collection.load()
    collection.delete(expr=f"doc_id == {doc_id}")
    collection.flush()
