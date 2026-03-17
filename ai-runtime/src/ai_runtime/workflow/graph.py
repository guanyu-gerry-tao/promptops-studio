"""
LangGraph workflow: RAG + JSON output template.

This module defines a 6-node DAG (Directed Acyclic Graph):

  route_intent → retrieve_kb → generate_json → validate_schema
                                                     ↓
                                               [pass] finalize
                                               [fail] repair_json → validate_schema (retry)

Each node is a plain function that:
  - Receives the full WorkflowState
  - Does its work (calling services, LLM, etc.)
  - Returns a partial dict with only the keys it wants to update

LangGraph merges the returned dict into the existing state automatically.

★ KEY DESIGN: Services (EmbeddingService, WeaviateService, etc.) are injected
  via closure — build_rag_workflow() captures them and passes to each node.
  This avoids global state and keeps the graph testable.
"""

import json
from loguru import logger
import time

import jsonschema
import openai
from langgraph.graph import StateGraph, END

from ai_runtime.config import Settings
from ai_runtime.services.embedding_service import EmbeddingService
from ai_runtime.services.weaviate_service import WeaviateService
from ai_runtime.services.rerank_service import RerankService
from ai_runtime.workflow.state import WorkflowState, TraceEntry


# Maximum number of repair attempts before giving up
MAX_REPAIR_ATTEMPTS = 2

# ★ Built-in JSON schemas for output validation.
#   In a real system these would come from a database; for MVP we hardcode two.
SCHEMAS: dict[str, dict] = {
    "qa_answer": {
        "type": "object",
        "properties": {
            "answer": {"type": "string"},
            "confidence": {"type": "number", "minimum": 0, "maximum": 1},
            "sources": {"type": "array", "items": {"type": "string"}},
        },
        "required": ["answer", "confidence", "sources"],
    },
}


# ──────────────────────────────────────────────────────
# Node functions — each returns a partial state update
# ──────────────────────────────────────────────────────

def _make_trace(node_name: str, input_summary: str, output_summary: str,
                start: float, token_count: int | None = None,
                citations: list[str] | None = None) -> TraceEntry:
    """
    Helper to build a TraceEntry with elapsed time.
    
    Args:
        node_name: Name of the workflow node
        input_summary: Brief description of inputs (truncated to 200 chars)
        output_summary: Brief description of outputs (truncated to 200 chars)
        start: Start timestamp from time.time()
        token_count: Optional token count from LLM response
        citations: Optional list of source citations
    """

    return TraceEntry(
        node_name=node_name,
        input_summary=input_summary[:200],
        output_summary=output_summary[:200],
        latency_ms=int((time.time() - start) * 1000),
        token_count=token_count,
        citations=citations,
    )


# ★ NODE 1: route_intent
def route_intent(state: WorkflowState) -> dict:
    """
    Decide whether this query needs knowledge base retrieval.

    For the RAG template, the answer is almost always "yes".
    A smarter version could use an LLM or keyword classifier to skip
    retrieval for simple greetings or off-topic queries.
    """
    start = time.time()
    # Simple heuristic: always retrieve for RAG workflow
    needs = True
    trace = _make_trace(
        "route_intent",
        f"user_input={state['user_input'][:80]}",
        f"needs_retrieval={needs}",
        start,
    )
    return {
        "needs_retrieval": needs,
        "trace": state.get("trace", []) + [trace],
    }


# ★ NODE 2: retrieve_kb
def _build_retrieve_kb(embedding_svc: EmbeddingService,
                       weaviate_svc: WeaviateService,
                       rerank_svc: RerankService,
                       settings: Settings):
    """Factory: returns a retrieve_kb node with injected services."""

    def retrieve_kb(state: WorkflowState) -> dict:
        start = time.time()

        query = state["user_input"]
        project_id = state["project_id"]

        # Step 1: Convert the user's text query into a vector embedding
        query_vector = embedding_svc.embed_single(query)

        # Step 2: Hybrid search — combines vector similarity + keyword matching.
        # alpha controls the balance (0 = pure keyword, 1 = pure vector).
        # If rerank is enabled, fetch more candidates (rerank_top_k) to feed the reranker;
        # otherwise fetch the final count (retrieve_top_k) directly.
        alpha = settings.weaviate_alpha
        top_k = settings.rerank_top_k if settings.rerank_enabled else settings.retrieve_top_k
        raw_results = weaviate_svc.hybrid_search(
            project_id=project_id,
            query=query,
            query_embedding=query_vector,
            alpha=alpha,
            top_k=top_k,
        )

        # Step 3 (optional): Rerank — use a cross-encoder model to re-score and
        # trim the candidate chunks down to the most relevant top_n.
        if settings.rerank_enabled and raw_results:
            raw_results = rerank_svc.rerank(
                query=query,
                chunks=raw_results,
                top_n=settings.rerank_top_n,
            )

        # Collect unique source titles for citation tracking
        citations = list({r["title"] for r in raw_results})
        trace = _make_trace(
            "retrieve_kb",
            f"query={query[:80]}, alpha={alpha}",
            f"{len(raw_results)} chunks retrieved",
            start,
            citations=citations,
        )
        return {
            "retrieved_chunks": raw_results,
            "trace": state.get("trace", []) + [trace],
        }

    return retrieve_kb


# ★ NODE 3: generate_json
def _build_generate_json(settings: Settings):
    """Factory: returns a generate_json node with injected settings."""

    def generate_json(state: WorkflowState) -> dict:
        start = time.time()

        # Step 1: Build context string from retrieved chunks.
        # Each chunk is formatted as "[Source: title]\ntext" so the LLM
        # knows which document each piece of information comes from.
        chunks = state.get("retrieved_chunks", [])
        context = "\n\n".join(
            f"[Source: {c['title']}]\n{c['text']}" for c in chunks
        )

        # Step 2: If a schema_id is specified, inject the JSON Schema into
        # the system prompt so the LLM knows the exact structure to output.
        schema_id = state.get("schema_id")
        schema_instruction = ""
        if schema_id and schema_id in SCHEMAS:
            schema_instruction = (
                f"\n\nYou MUST return a valid JSON object matching this schema:\n"
                f"```json\n{json.dumps(SCHEMAS[schema_id], indent=2)}\n```"
            )

        # Step 3: Assemble the full prompt.
        # system_prompt = role + rules + optional schema constraint
        # user_prompt   = retrieved context + user's original question
        system_prompt = (
            "You are a helpful assistant that answers questions based on "
            "the provided context documents.\n\n"
            "## Rules\n"
            "- Answer ONLY from the context below. Do not use outside knowledge.\n"
            "- Return your answer as a JSON object.\n"
            "- If the context is insufficient, set confidence to 0 and explain in the answer field."
            f"{schema_instruction}"
        )

        user_prompt = f"## Context\n\n{context}\n\n## Question\n\n{state['user_input']}"

        # Step 4: Call OpenAI with JSON mode enabled.
        # response_format={"type": "json_object"} guarantees syntactically valid JSON,
        # but does NOT guarantee the JSON matches our schema — that's validate_schema's job.
        client = openai.OpenAI(api_key=settings.openai_api_key)
        response = client.chat.completions.create(
            model=settings.openai_chat_model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            response_format={"type": "json_object"},
        )

        raw_content = response.choices[0].message.content
        token_count = response.usage.total_tokens if response.usage else None

        # Step 5: Parse the JSON string into a dict.
        # If parsing fails (shouldn't happen with JSON mode, but defensive), set draft to None.
        # validate_schema will catch this and mark it as FAILED.
        try:
            draft = json.loads(raw_content)
        except json.JSONDecodeError:
            draft = None

        trace = _make_trace(
            "generate_json",
            f"chunks={len(chunks)}, schema={schema_id}",
            raw_content[:200] if raw_content else "null",
            start,
            token_count=token_count,
        )
        return {
            "draft_output_json": draft,
            "repair_attempts": 0,       # Reset counter for the validate→repair loop
            "trace": state.get("trace", []) + [trace],
        }

    return generate_json


# ★ NODE 4: validate_schema
def validate_schema(state: WorkflowState) -> dict:
    """
    Validate draft_output_json against the JSON Schema (if schema_id is set).
    If no schema is configured, auto-pass.
    """
    start = time.time()
    draft = state.get("draft_output_json")
    schema_id = state.get("schema_id")

    # Case 1: LLM returned nothing parseable — immediate fail
    if draft is None:
        trace = _make_trace("validate_schema", "draft=None", "FAIL: no output", start)
        return {
            "validation_passed": False,
            "validation_errors": ["LLM returned no parseable JSON"],
            "trace": state.get("trace", []) + [trace],
        }

    # Case 2: No schema configured — skip validation, auto-pass
    if not schema_id or schema_id not in SCHEMAS:
        trace = _make_trace("validate_schema", f"schema={schema_id}", "PASS (no schema)", start)
        return {
            "validation_passed": True,
            "validation_errors": [],
            "trace": state.get("trace", []) + [trace],
        }

    # Case 3: Validate draft against JSON Schema using Draft7 spec.
    # iter_errors() returns all violations (missing fields, wrong types, etc.)
    schema = SCHEMAS[schema_id]
    validator = jsonschema.Draft7Validator(schema)
    errors = [e.message for e in validator.iter_errors(draft)]

    passed_flag = len(errors) == 0
    trace = _make_trace(
        "validate_schema",
        f"schema={schema_id}",
        f"{'PASS' if passed_flag else 'FAIL'}: {errors[:3]}",
        start,
    )
    return {
        "validation_passed": passed_flag,
        "validation_errors": errors,
        "trace": state.get("trace", []) + [trace],
    }


# ★ NODE 5: repair_json
def _build_repair_json(settings: Settings):
    """Factory: returns a repair_json node with injected settings."""

    def repair_json(state: WorkflowState) -> dict:
        start = time.time()
        attempts = state.get("repair_attempts", 0) + 1
        errors = state.get("validation_errors", [])
        draft = state.get("draft_output_json", {})
        schema_id = state.get("schema_id", "")

        schema = SCHEMAS.get(schema_id, {})

        # Build a repair prompt: give the LLM the broken JSON, the target schema,
        # and the specific validation errors so it knows exactly what to fix.
        system_prompt = (
            "You are a JSON repair assistant. Fix the JSON object to match the schema.\n"
            "Return ONLY the corrected JSON object, nothing else."
        )
        user_prompt = (
            f"## Current JSON\n```json\n{json.dumps(draft, indent=2)}\n```\n\n"
            f"## Target Schema\n```json\n{json.dumps(schema, indent=2)}\n```\n\n"
            f"## Validation Errors\n" + "\n".join(f"- {e}" for e in errors)
        )

        # Call LLM to fix the JSON (same JSON mode as generate_json)
        client = openai.OpenAI(api_key=settings.openai_api_key)
        response = client.chat.completions.create(
            model=settings.openai_chat_model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            response_format={"type": "json_object"},
        )

        raw_content = response.choices[0].message.content
        token_count = response.usage.total_tokens if response.usage else None

        # Parse the repaired JSON; if even repair fails to parse, keep the old draft
        # so validate_schema can report the original errors on the next pass.
        try:
            repaired = json.loads(raw_content)
        except json.JSONDecodeError:
            repaired = draft

        trace = _make_trace(
            "repair_json",
            f"attempt={attempts}, errors={len(errors)}",
            raw_content[:200] if raw_content else "null",
            start,
            token_count=token_count,
        )
        return {
            "draft_output_json": repaired,
            "repair_attempts": attempts,
            "trace": state.get("trace", []) + [trace],
        }

    return repair_json


# ★ NODE 6: finalize
def finalize(state: WorkflowState) -> dict:
    """
    Final node — package the result.
    If validation passed, promote draft to final output.
    Otherwise, mark as FAILED.
    """
    start = time.time()
    passed = state.get("validation_passed", False)

    if passed:
        status = "SUCCESS"
        final = state.get("draft_output_json")
        error = None
    else:
        status = "FAILED"
        final = None
        error = "; ".join(state.get("validation_errors", ["Unknown error"]))

    trace = _make_trace("finalize", f"validation_passed={passed}", f"status={status}", start)
    return {
        "final_output_json": final,
        "status": status,
        "error_message": error,
        "trace": state.get("trace", []) + [trace],
    }


# ──────────────────────────────────────────────────────
# ★ Graph builder — wires nodes + edges + conditions
# ──────────────────────────────────────────────────────

def build_rag_workflow(
    embedding_svc: EmbeddingService,
    weaviate_svc: WeaviateService,
    rerank_svc: RerankService,
    settings: Settings,
):
    """
    Build and compile the RAG + JSON workflow graph.

    Returns a compiled LangGraph that can be invoked with:
        result = graph.invoke(initial_state)

    The graph looks like:

        route_intent
             │
             ├─ needs_retrieval=True  → retrieve_kb → generate_json → validate_schema
             │                                                              │
             │                                              [pass] → finalize → END
             │                                              [fail] → repair_json → validate_schema
             │                                                        (max 2 retries, then finalize)
             │
             └─ needs_retrieval=False → generate_json → ...same as above
    """
    # --- Phase 1: Create node functions ---
    # Nodes that need external services are built via closure factories;
    # plain nodes (route_intent, validate_schema, finalize) are used directly.
    retrieve_kb = _build_retrieve_kb(embedding_svc, weaviate_svc, rerank_svc, settings)
    generate_json = _build_generate_json(settings)
    repair_json = _build_repair_json(settings)

    # --- Phase 2: Register nodes ---
    # StateGraph is parameterized with WorkflowState so LangGraph knows the state shape.
    graph = StateGraph(WorkflowState)

    graph.add_node("route_intent", route_intent)
    graph.add_node("retrieve_kb", retrieve_kb)
    graph.add_node("generate_json", generate_json)
    graph.add_node("validate_schema", validate_schema)
    graph.add_node("repair_json", repair_json)
    graph.add_node("finalize", finalize)

    # --- Phase 3: Wire edges ---

    # Entry point: every execution starts at route_intent
    graph.set_entry_point("route_intent")

    # Conditional edge 1: route_intent → retrieve_kb or generate_json
    # The routing function reads needs_retrieval from state and returns the next node name.
    # The dict maps return values to actual node names (required by LangGraph).
    # NOTE: This could also be written as a lambda for brevity:
    #   lambda s: "retrieve_kb" if s.get("needs_retrieval") else "generate_json"
    def after_route_intent(state: WorkflowState) -> str:
        if state.get("needs_retrieval"):
            return "retrieve_kb"
        return "generate_json"

    graph.add_conditional_edges(
        "route_intent",
        after_route_intent,
        {"retrieve_kb": "retrieve_kb", "generate_json": "generate_json"},
    )

    # Linear edges: retrieve → generate → validate (straightforward pipeline)
    graph.add_edge("retrieve_kb", "generate_json")
    graph.add_edge("generate_json", "validate_schema")

    # Conditional edge 2: validate_schema → finalize or repair_json
    # Three-way routing: passed → finalize, failed + retries left → repair, exhausted → finalize
    def after_validation(state: WorkflowState) -> str:
        if state.get("validation_passed"):
            return "finalize"
        if state.get("repair_attempts", 0) >= MAX_REPAIR_ATTEMPTS:
            logger.warning("Max repair attempts reached, finalizing as FAILED")
            return "finalize"
        return "repair_json"

    graph.add_conditional_edges(
        "validate_schema",
        after_validation,
        {"finalize": "finalize", "repair_json": "repair_json"},
    )

    # Back-edge: repair → validate creates a bounded loop (max MAX_REPAIR_ATTEMPTS)
    graph.add_edge("repair_json", "validate_schema")

    # Terminal edge: finalize → END stops the graph execution
    graph.add_edge("finalize", END)

    # --- Phase 4: Compile ---
    # compile() validates the graph structure (no dangling nodes, no missing edges)
    # and returns an executable object that supports graph.invoke(initial_state).
    return graph.compile()
