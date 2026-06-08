from fastapi import APIRouter

from app.core.config import settings

router = APIRouter(prefix="/ai", tags=["health"])


@router.get("/health")
def health():
    return {"status": "ok", "modelProvider": settings.default_model}
