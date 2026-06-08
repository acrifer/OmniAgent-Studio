from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes_assistant import router as assistant_router
from app.api.routes_health import router as health_router
from app.api.routes_observability import router as observability_router
from app.api.routes_omni import router as omni_router
from app.api.routes_workflow import router as workflow_router
from app.core.config import settings

app = FastAPI(title=settings.app_name)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(workflow_router)
app.include_router(assistant_router)
app.include_router(observability_router)
app.include_router(omni_router)
