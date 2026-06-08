from datetime import datetime, timezone
from typing import Any

from fastapi import APIRouter, Header, HTTPException

from app.core.config import settings
from app.schemas.workflow import (
    KnowledgeIngestRequest,
    KnowledgeIngestResponse,
    KnowledgeRetrieveRequest,
    KnowledgeRetrieveResponse,
    McpCallRequest,
    StartOmniAgentRunRequest,
    StartOmniAgentRunResponse,
    TaskStatus,
)
from app.services.omni_agent_service import omni_agent_service

router = APIRouter(tags=["omni-agent"])


def check_token(token: str | None) -> None:
    if token != settings.internal_token:
        raise HTTPException(status_code=401, detail="invalid internal token")


@router.post("/ai/agent-runs/start", response_model=StartOmniAgentRunResponse)
async def start_agent_run(request: StartOmniAgentRunRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    try:
        ai_task_id = await omni_agent_service.start(request)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return StartOmniAgentRunResponse(aiTaskId=ai_task_id, status="RUNNING")


@router.get("/ai/agent-runs/{ai_task_id}/status", response_model=TaskStatus)
async def agent_run_status(ai_task_id: str, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    return omni_agent_service.status(ai_task_id)


@router.post("/ai/knowledge/ingest", response_model=KnowledgeIngestResponse)
async def ingest_knowledge(request: KnowledgeIngestRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    return await omni_agent_service.ingest(request)


@router.post("/ai/knowledge/retrieve", response_model=KnowledgeRetrieveResponse)
async def retrieve_knowledge(request: KnowledgeRetrieveRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    return await omni_agent_service.retrieve(request)


@router.post("/ai/tools/mcp-call")
async def mcp_call(request: McpCallRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    try:
        return await omni_agent_service.mcp_call(request)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/ai/tools/demo/echo")
async def demo_echo_tool(payload: dict[str, Any]):
    """Demo HTTP tool for presentations. Configure this endpoint in the Tool page."""
    tool_name = payload.get("toolName") or "echo-tool"
    input_payload = payload.get("input", payload)
    return {
        "toolName": tool_name,
        "status": "SUCCESS",
        "message": "Echo demo tool executed successfully.",
        "received": input_payload,
        "serverTime": datetime.now(timezone.utc).isoformat(),
    }


@router.get("/ai/health/models")
async def model_health(x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    return {
        "deepseek": {"configured": bool(settings.deepseek_api_key), "baseUrl": settings.deepseek_base_url},
        "qwen": {"configured": bool(settings.qwen_api_key), "baseUrl": settings.qwen_base_url},
        "openai": {"configured": bool(settings.openai_api_key), "baseUrl": settings.openai_base_url},
        "search": {"configured": bool(settings.tavily_api_key), "provider": "tavily"},
        "qdrant": {"configured": bool(settings.qdrant_url), "baseUrl": settings.qdrant_url},
        "embedding": {"model": settings.embedding_model},
        "vision": {"model": settings.vision_model},
        "langfuse": {"configured": bool(settings.langfuse_enabled)},
    }
