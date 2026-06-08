from fastapi import APIRouter, Header, HTTPException

from app.core.config import settings
from app.observability.langfuse_client import trace_recorder
from app.schemas.workflow import LangfuseScoreRequest

router = APIRouter(prefix="/ai/observability", tags=["observability"])


def check_token(token: str | None) -> None:
    if token != settings.internal_token:
        raise HTTPException(status_code=401, detail="invalid internal token")


@router.post("/score")
async def create_score(request: LangfuseScoreRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    accepted = trace_recorder.score(
        request.traceId,
        request.observationId,
        request.name,
        request.value,
        request.comment,
        request.metadata,
    )
    return {"accepted": accepted, "langfuseEnabled": trace_recorder.enabled}
