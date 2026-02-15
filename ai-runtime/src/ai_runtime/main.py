"""
AI Runtime Service - Main Application

FastAPI application for AI workflow execution and knowledge base management.
"""

from datetime import datetime
from fastapi import FastAPI

from ai_runtime.routers.index_router import router as index_router
from ai_runtime.routers.retrieve_router import router as retrieve_router

# Create FastAPI application instance
app = FastAPI(
    title="AI Runtime Service",
    description="AI Runtime Service with FastAPI and LangChain",
    version="0.1.0",
)

app.include_router(index_router)
app.include_router(retrieve_router)


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
