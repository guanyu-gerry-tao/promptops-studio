"""
POST /execute-case endpoint.

Runs a single test case through a LangGraph workflow and returns
the structured output along with a node-level execution trace.
"""

import logging

from fastapi import APIRouter, Depends

from ai_runtime.config import Settings
from ai_runtime.models import (
    ExecuteCaseRequest,
    ExecuteCaseResponse,
    TraceRecord,
)
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.services.weaviate_service import WeaviateService
from ai_runtime.services.rerank_service import RerankService
from ai_runtime.dependencies import (
    get_embedding_service,
    get_weaviate_service,
    get_rerank_service,
    get_settings,
)
from ai_runtime.workflow.graph import build_rag_workflow

logger = logging.getLogger(__name__)

# a APIRouter can be thought of as a "sub-application" that defines a group of related endpoints.
# at the main.py, we can invoke app.include_router(router) to register these endpoints under the main FastAPI app.
# example: app.include_router(execute_router.router, prefix="/execute") will make the endpoint available at POST /execute/execute-case
router = APIRouter(tags=["workflow"])

# Cache the compiled graph so we don't rebuild it on every request.
# The graph itself is stateless — all per-request data lives in WorkflowState.
_compiled_graph_cache: dict[str, object] = {}


def _get_graph(
    embedding_svc: EmbeddingService,
    weaviate_svc: WeaviateService,
    rerank_svc: RerankService,
    settings: Settings,
):
    """Get or build the compiled workflow graph (singleton per workflow_id)."""
    key = "rag_json"  # MVP: only one template, hardcoded for now. 

    # add the key if not exist, and build the graph and store in cache
    if key not in _compiled_graph_cache:
        _compiled_graph_cache[key] = build_rag_workflow(
            embedding_svc, weaviate_svc, rerank_svc, settings
        )
    return _compiled_graph_cache[key]


# notice the "/execute-case" path is relative to the prefix we set in main.py when including the router
@router.post("/execute-case", response_model=ExecuteCaseResponse)
def execute_case(
    request: ExecuteCaseRequest,
    embedding_svc: EmbeddingService = Depends(get_embedding_service),
    weaviate_svc: WeaviateService = Depends(get_weaviate_service),
    rerank_svc: RerankService = Depends(get_rerank_service),
    settings: Settings = Depends(get_settings),
) -> ExecuteCaseResponse:
    """
    Execute a single case through a LangGraph workflow.

    Flow:
      1. Build initial WorkflowState from the request
      2. Invoke the compiled LangGraph (6 nodes)
      3. Convert the final state into ExecuteCaseResponse
    """
    logger.info(
        "POST /execute-case: project=%d, workflow=%s, case=%s",
        request.project_id, request.workflow_id, request.case_id,
    )

    graph = _get_graph(embedding_svc, weaviate_svc, rerank_svc, settings)

    # ★ Build initial state — only set the input fields
    initial_state = {
        "project_id": request.project_id,
        "workflow_id": request.workflow_id,
        "case_id": request.case_id,
        "user_input": request.user_input,
        "schema_id": request.schema_id,
        "trace": [],
    }

    # ★ Run the graph — LangGraph handles node execution order
    final_state = graph.invoke(initial_state)

    # Collect citations from all trace entries
    # FUTURE: make citation fine, e.g. inline citation, so user can directly know which sentence is from which document
    all_citations: list[str] = []
    for t in final_state.get("trace", []):
        if t.get("citations"):
            all_citations.extend(t["citations"])
    unique_citations = list(dict.fromkeys(all_citations))  # dedupe, preserve order

    # Convert trace dicts to TraceRecord models
    trace_records = [TraceRecord(**t) for t in final_state.get("trace", [])]

    return ExecuteCaseResponse(
        project_id=request.project_id,
        workflow_id=request.workflow_id,
        case_id=request.case_id,
        status=final_state.get("status", "FAILED"),
        output_json=final_state.get("final_output_json"),
        error_message=final_state.get("error_message"),
        trace=trace_records,
        citations=unique_citations,
    )