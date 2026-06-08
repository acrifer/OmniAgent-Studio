from fastapi import APIRouter, Header, HTTPException

from app.api.routes_workflow import check_token
from app.schemas.workflow import RequirementDraftRequest, RequirementDraftResponse
from app.services.assistant_service import assistant_service

router = APIRouter(prefix="/ai/assistants", tags=["assistant"])


@router.post("/requirement-draft", response_model=RequirementDraftResponse)
async def requirement_draft(request: RequirementDraftRequest, x_internal_token: str | None = Header(default=None)):
    check_token(x_internal_token)
    if not request.projectTitle.strip():
        raise HTTPException(status_code=400, detail="projectTitle is required")
    try:
        return await assistant_service.generate_requirement_draft(request)
    except RuntimeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
