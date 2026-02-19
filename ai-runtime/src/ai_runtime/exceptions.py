"""
Custom exceptions for AI Runtime.

Why custom exceptions?
  - Without them, a Milvus connection error and an OpenAI API error
    look the same to the caller: just a generic Python traceback.
  - With them, the router can catch EmbeddingError vs MilvusError
    and return a clear, specific error message to the API caller.

This is the Python equivalent of defining custom exception classes
in Java (like ServiceException extends RuntimeException).
"""


class AIRuntimeError(Exception):
    """
    Base exception for all AI Runtime errors.

    All custom exceptions inherit from this, so the router can do:
        except AIRuntimeError as e:
    to catch any of our known errors in one place.
    """
    pass


class EmbeddingError(AIRuntimeError):
    """
    Raised when the OpenAI embedding API call fails.

    Common causes:
      - Invalid or expired API key
      - Rate limit exceeded
      - Network timeout
      - Empty text input
    """
    pass


class MilvusError(AIRuntimeError):
    """
    Raised when a Milvus operation fails.

    Common causes:
      - Milvus server is down / unreachable
      - Collection creation failed
      - Insert or search operation failed
    """
    pass


class WeaviateError(AIRuntimeError):
    """
    Raised when a Weaviate operation fails.

    Common causes:
      - Weaviate server is down / unreachable
      - Collection creation failed
      - Insert or hybrid search operation failed
    """
    pass


class DocumentProcessingError(AIRuntimeError):
    """
    Raised when the document processing pipeline fails.

    This usually wraps an EmbeddingError or MilvusError with
    additional context (which document, which step failed).
    """
    pass
