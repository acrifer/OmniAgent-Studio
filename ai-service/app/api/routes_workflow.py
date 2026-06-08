from fastapi import APIRouter, Header, HTTPException

from app.core.config import settings
from app.schemas.workflow import RegenerateWorkflowRequest, StartWorkflowRequest, StartWorkflowResponse, TaskStatus
from app.services.workflow_service import workflow_service

router = APIRouter(prefix="/ai/workflows", tags=["workflow"])


def check_token(token: str | None) -> None:
    if token != settings.internal_token:
        raise HTTPException(status_code=401, detail="invalid internal token")


@router.post("/start", response_model=StartWorkflowResponse)
async def start_workflow(request: StartWorkflowRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    try:
        ai_task_id = await workflow_service.start(request)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return StartWorkflowResponse(aiTaskId=ai_task_id, status="RUNNING")


@router.get("/{ai_task_id}/status", response_model=TaskStatus)
async def workflow_status(ai_task_id: str, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    return workflow_service.status(ai_task_id)


@router.post("/{ai_task_id}/regenerate", response_model=StartWorkflowResponse)
async def regenerate(ai_task_id: str, request: RegenerateWorkflowRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    try:
        new_ai_task_id = await workflow_service.regenerate(ai_task_id, request)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return StartWorkflowResponse(aiTaskId=new_ai_task_id, status="RUNNING")
