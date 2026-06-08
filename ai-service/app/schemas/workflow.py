from pydantic import BaseModel, Field
from typing import Any


class StartWorkflowRequest(BaseModel):
    projectId: int
    taskId: int
    projectName: str
    requirementText: str
    documents: list[str] = Field(default_factory=list)
    techStack: str
    modelName: str = "deepseek-v4-flash"
    callbackUrl: str
    previousResults: dict[str, str] = Field(default_factory=dict)


class StartWorkflowResponse(BaseModel):
    aiTaskId: str
    status: str


class TaskStatus(BaseModel):
    aiTaskId: str
    status: str
    progress: int
    nodes: dict[str, str] = Field(default_factory=dict)


class RegenerateWorkflowRequest(StartWorkflowRequest):
    targetAgent: str


class LangfuseScoreRequest(BaseModel):
    traceId: str | None = None
    observationId: str | None = None
    name: str = "user_feedback"
    value: float
    comment: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class AgentOutput(BaseModel):
    agent_type: str
    content_json: dict[str, Any]
    content_markdown: str
    prompt_tokens: int
    completion_tokens: int
    latency_ms: int


class RequirementDraftRequest(BaseModel):
    projectTitle: str
    projectDescription: str | None = None
    techStack: str | None = None
    modelName: str = "deepseek-v4-flash"


class RequirementDraftResponse(BaseModel):
    requirementText: str
    outline: list[str]
    suggestedModules: list[str]
    techRisks: list[str]


class StartOmniAgentRunRequest(BaseModel):
    conversationId: int
    runId: int
    messageId: int | None = None
    question: str
    mode: str = "AUTO"
    knowledgeBaseId: int | None = None
    documents: list[str] = Field(default_factory=list)
    files: list[dict[str, Any]] = Field(default_factory=list)
    modelName: str = "deepseek-v4-flash"
    callbackUrl: str


class StartOmniAgentRunResponse(BaseModel):
    aiTaskId: str
    status: str


class KnowledgeIngestRequest(BaseModel):
    knowledgeBaseId: int
    documentId: int
    text: str


class KnowledgeIngestResponse(BaseModel):
    status: str
    chunkCount: int = 0
    message: str | None = None


class KnowledgeRetrieveRequest(BaseModel):
    knowledgeBaseId: int
    query: str
    topK: int = 5


class KnowledgeRetrieveResponse(BaseModel):
    chunks: list[dict[str, Any]] = Field(default_factory=list)


class McpCallRequest(BaseModel):
    toolName: str
    endpoint: str | None = None
    inputJson: dict[str, Any] = Field(default_factory=dict)
