"""
AI Runtime Service - Main Application

FastAPI application for AI workflow execution and knowledge base management.
"""

import sys
from datetime import datetime

from loguru import logger

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from ai_runtime.routers.index_router import router as index_router
from ai_runtime.routers.retrieve_router import router as retrieve_router
from ai_runtime.routers.execute_router import router as execute_router
from ai_runtime.exceptions import AIRuntimeError, EmbeddingError, MilvusError

# Configure Loguru: remove default handler, add JSON sink to stdout
logger.remove()
logger.add(sys.stdout, serialize=True, level="INFO")

# Create FastAPI application instance
app = FastAPI(
    title="AI Runtime Service",
    description="AI Runtime Service with FastAPI and LangChain",
    version="0.1.0",
)


# ──────────────────────────────────────
# Global exception handlers
# ──────────────────────────────────────
# This is the Python equivalent of Spring's @ControllerAdvice.
# Instead of each router catching exceptions individually,
# we handle them in ONE place and return clean JSON responses.


@app.exception_handler(EmbeddingError)
async def embedding_error_handler(request: Request, exc: EmbeddingError):
    """Handle OpenAI embedding failures → 502 (upstream service failed)."""
    logger.error("EmbeddingError on %s: %s", request.url.path, exc)
    return JSONResponse(
        status_code=502,
        content={"error": "embedding_error", "message": str(exc)},
    )


@app.exception_handler(MilvusError)
async def milvus_error_handler(request: Request, exc: MilvusError):
    """Handle Milvus failures → 502 (upstream service failed)."""
    logger.error("MilvusError on %s: %s", request.url.path, exc)
    return JSONResponse(
        status_code=502,
        content={"error": "milvus_error", "message": str(exc)},
    )


@app.exception_handler(AIRuntimeError)
async def ai_runtime_error_handler(request: Request, exc: AIRuntimeError):
    """Catch-all for any other custom errors → 500."""
    logger.error("AIRuntimeError on %s: %s", request.url.path, exc)
    return JSONResponse(
        status_code=500,
        content={"error": "internal_error", "message": str(exc)},
    )


# ──────────────────────────────────────
# Routers
# ──────────────────────────────────────

app.include_router(index_router)
app.include_router(retrieve_router)
app.include_router(execute_router)


# ──────────────────────────────────────
# Built-in endpoints
# ──────────────────────────────────────


@app.get("/health")
def health_check():
    """
    Health check endpoint.

    Returns the service status, timestamp, and service name.
    """
    return {
        "status": "OK",
        "timestamp": datetime.now().isoformat(),
        "service": "ai-runtime",
    }


@app.get("/")
def root():
    """
    Root endpoint.

    Returns basic API information.
    """
    return {
        "message": "AI Runtime Service",
        "version": "0.1.0",
        "docs": "/docs",
    }
