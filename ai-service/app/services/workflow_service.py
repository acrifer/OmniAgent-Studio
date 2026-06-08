from __future__ import annotations

import asyncio
import json
import operator
import uuid
from dataclasses import dataclass, field
from typing import Annotated, Any, Dict, TypedDict

import httpx
from langgraph.graph import END, START, StateGraph

from app.core.config import settings
from app.llm.llm_client import llm_client
from app.observability.langfuse_client import trace_recorder
from app.schemas.workflow import RegenerateWorkflowRequest, StartWorkflowRequest, TaskStatus


PRIMARY_AGENT = "PM"
PARALLEL_AGENTS = ["ARCHITECT", "FRONTEND", "BACKEND", "TEST", "SECURITY"]
AGENT_ORDER = [PRIMARY_AGENT, *PARALLEL_AGENTS, "SUMMARY"]
FULL_PROGRESS = {
    "PM": (10, 24),
    "ARCHITECT": (32, 70),
    "FRONTEND": (34, 72),
    "BACKEND": (36, 74),
    "TEST": (38, 76),
    "SECURITY": (40, 78),
    "SUMMARY": (84, 96),
}
REGENERATE_PROGRESS = {
    "PM": (20, 58),
    "ARCHITECT": (20, 58),
    "FRONTEND": (20, 58),
    "BACKEND": (20, 58),
    "TEST": (20, 58),
    "SECURITY": (20, 58),
    "SUMMARY": (68, 94),
}


class WorkflowState(TypedDict, total=False):
    ai_task_id: str
    request: StartWorkflowRequest
    trace_id: str
    mode: str
    completed_agents: Annotated[list[str], operator.add]
    pm_output: dict[str, Any]
    architect_output: dict[str, Any]
    frontend_output: dict[str, Any]
    backend_output: dict[str, Any]
    test_output: dict[str, Any]
    security_output: dict[str, Any]
    summary_output: dict[str, Any]


@dataclass
class RuntimeTask:
    ai_task_id: str
    status: str = "RUNNING"
    progress: int = 0
    nodes: Dict[str, str] = field(default_factory=dict)


class WorkflowService:
    def __init__(self) -> None:
        self.tasks: dict[str, RuntimeTask] = {}
        self.full_graph = self._build_full_graph()

    async def start(self, request: StartWorkflowRequest) -> str:
        ai_task_id = str(uuid.uuid4())
        self.tasks[ai_task_id] = RuntimeTask(ai_task_id=ai_task_id)
        asyncio.create_task(self._run_graph(ai_task_id, request, self.full_graph, "FULL"))
        return ai_task_id

    async def regenerate(self, previous_ai_task_id: str, request: RegenerateWorkflowRequest) -> str:
        target_agent = request.targetAgent.upper()
        if target_agent not in AGENT_ORDER:
            raise RuntimeError(f"不支持重生成 Agent：{request.targetAgent}")
        ai_task_id = str(uuid.uuid4())
        self.tasks[ai_task_id] = RuntimeTask(ai_task_id=ai_task_id, nodes={target_agent: "PENDING", "SUMMARY": "PENDING"})
        graph = self._build_regenerate_graph(target_agent)
        asyncio.create_task(self._run_graph(ai_task_id, request, graph, "REGENERATE", previous_ai_task_id))
        return ai_task_id

    def status(self, ai_task_id: str) -> TaskStatus:
        task = self.tasks.get(ai_task_id)
        if task is None:
            return TaskStatus(aiTaskId=ai_task_id, status="NOT_FOUND", progress=0, nodes={})
        return TaskStatus(aiTaskId=ai_task_id, status=task.status, progress=task.progress, nodes=task.nodes)

    def _build_full_graph(self):
        graph = StateGraph(WorkflowState)
        for agent_type in AGENT_ORDER:
            graph.add_node(agent_type, self._agent_node(agent_type))
        graph.add_edge(START, PRIMARY_AGENT)
        for agent_type in PARALLEL_AGENTS:
            graph.add_edge(PRIMARY_AGENT, agent_type)
            graph.add_edge(agent_type, "SUMMARY")
        graph.add_edge("SUMMARY", END)
        return graph.compile()

    def _build_regenerate_graph(self, target_agent: str):
        graph = StateGraph(WorkflowState)
        if target_agent != "SUMMARY":
            graph.add_node(target_agent, self._agent_node(target_agent))
            graph.add_node("SUMMARY", self._agent_node("SUMMARY"))
            graph.add_edge(START, target_agent)
            graph.add_edge(target_agent, "SUMMARY")
            graph.add_edge("SUMMARY", END)
        else:
            graph.add_node("SUMMARY", self._agent_node("SUMMARY"))
            graph.add_edge(START, "SUMMARY")
            graph.add_edge("SUMMARY", END)
        return graph.compile()

    def _agent_node(self, agent_type: str):
        async def node(state: WorkflowState) -> dict[str, Any]:
            request = state["request"]
            ai_task_id = state["ai_task_id"]
            trace_id = state["trace_id"]
            runtime = self.tasks[ai_task_id]
            progress_start, progress_success = self._progress(agent_type, state.get("mode", "FULL"))
            runtime.nodes[agent_type] = "RUNNING"
            await self._event(request, ai_task_id, "NODE_STARTED", agent_type, "RUNNING", progress_start, langfuse_trace_id=trace_id)
            context = self._context_from_state(state, request)
            prompt = llm_client.build_prompt(agent_type, request.projectName, request.requirementText, context)
            try:
                with trace_recorder.generation(trace_id, agent_type, prompt, request.modelName) as observation_id:
                    output = await llm_client.generate_agent_output(
                        agent_type,
                        request.projectName,
                        request.requirementText,
                        context,
                        request.modelName,
                        prompt=prompt,
                    )
                    trace_recorder.update_generation(
                        observation_id,
                        output.content_json,
                        {
                            "promptTokens": output.prompt_tokens,
                            "completionTokens": output.completion_tokens,
                            "totalTokens": output.prompt_tokens + output.completion_tokens,
                        },
                        output.latency_ms,
                    )
                    await self._event(
                        request,
                        ai_task_id,
                        "NODE_SUCCESS",
                        agent_type,
                        "SUCCESS",
                        progress_success,
                        content_json=output.content_json,
                        content_markdown=output.content_markdown,
                        model_name=request.modelName,
                        prompt_version="v1",
                        langfuse_trace_id=trace_id,
                        trace_observation_id=observation_id,
                        prompt_tokens=output.prompt_tokens,
                        completion_tokens=output.completion_tokens,
                        latency_ms=output.latency_ms,
                    )
            except Exception as exc:
                runtime.nodes[agent_type] = "FAILED"
                await self._event(request, ai_task_id, "NODE_FAILED", agent_type, "FAILED", progress_start, error_message=str(exc), langfuse_trace_id=trace_id)
                raise
            runtime.nodes[agent_type] = "SUCCESS"
            return {self._output_key(agent_type): output.content_json, "completed_agents": [agent_type]}

        return node

    async def _run_graph(self, ai_task_id: str, request: StartWorkflowRequest, graph, mode: str, previous_ai_task_id: str | None = None) -> None:
        runtime = self.tasks[ai_task_id]
        try:
            metadata = {
                "task_id": request.taskId,
                "project_id": request.projectId,
                "mode": mode,
                "previous_ai_task_id": previous_ai_task_id,
                "langfuse_enabled": trace_recorder.enabled,
            }
            with trace_recorder.trace("devmind-agent-workflow", metadata) as trace_id:
                await self._event(request, ai_task_id, "TASK_STARTED", None, "RUNNING", 5, langfuse_trace_id=trace_id)
                initial_state: WorkflowState = {
                    "ai_task_id": ai_task_id,
                    "request": request,
                    "trace_id": trace_id,
                    "mode": mode,
                    "completed_agents": [],
                }
                for key, value in self._parse_previous_results(request.previousResults).items():
                    if key in AGENT_ORDER:
                        initial_state[self._output_key(key)] = value
                await graph.ainvoke(initial_state)
                runtime.status = "SUCCESS"
                runtime.progress = 100
                await self._event(request, ai_task_id, "TASK_FINISHED", None, "SUCCESS", 100, langfuse_trace_id=trace_id)
        except Exception as exc:
            runtime.status = "FAILED"
            runtime.progress = 100
            await self._event(request, ai_task_id, "TASK_FAILED", None, "FAILED", 100, error_message=str(exc))

    def _context_from_state(self, state: WorkflowState, request: StartWorkflowRequest) -> dict[str, Any]:
        context: dict[str, Any] = {
            "TECH_STACK": request.techStack,
            "DOCUMENTS": [text[:4000] for text in request.documents if text],
        }
        for agent_type in AGENT_ORDER:
            output_key = self._output_key(agent_type)
            if state.get(output_key) is not None:
                context[agent_type] = state[output_key]
        return context

    def _parse_previous_results(self, previous_results: dict[str, str] | None) -> dict[str, Any]:
        parsed: dict[str, Any] = {}
        for agent_type, raw in (previous_results or {}).items():
            try:
                parsed[agent_type] = json.loads(raw)
            except (TypeError, json.JSONDecodeError):
                parsed[agent_type] = raw
        return parsed

    def _progress(self, agent_type: str, mode: str) -> tuple[int, int]:
        return (REGENERATE_PROGRESS if mode == "REGENERATE" else FULL_PROGRESS)[agent_type]

    def _output_key(self, agent_type: str) -> str:
        return f"{agent_type.lower()}_output"

    async def _event(
        self,
        request: StartWorkflowRequest,
        ai_task_id: str,
        event_type: str,
        agent_type: str | None,
        status: str,
        progress: int,
        **kwargs,
    ) -> None:
        payload = {
            "aiTaskId": ai_task_id,
            "taskId": request.taskId,
            "projectId": request.projectId,
            "eventType": event_type,
            "agentType": agent_type,
            "status": status,
            "progress": progress,
            "contentJson": None,
            "contentMarkdown": None,
            "errorMessage": kwargs.get("error_message"),
            "modelName": kwargs.get("model_name"),
            "promptVersion": kwargs.get("prompt_version"),
            "langfuseTraceId": kwargs.get("langfuse_trace_id"),
            "traceObservationId": kwargs.get("trace_observation_id"),
            "promptTokens": kwargs.get("prompt_tokens"),
            "completionTokens": kwargs.get("completion_tokens"),
            "latencyMs": kwargs.get("latency_ms"),
            "metadata": {"langfuseEnabled": trace_recorder.enabled},
        }
        if kwargs.get("content_json") is not None:
            payload["contentJson"] = json.dumps(kwargs["content_json"], ensure_ascii=False)
        if kwargs.get("content_markdown") is not None:
            payload["contentMarkdown"] = kwargs["content_markdown"]

        runtime = self.tasks[ai_task_id]
        runtime.progress = progress
        if event_type == "TASK_FINISHED":
            runtime.status = "SUCCESS"
        elif event_type == "TASK_FAILED":
            runtime.status = "FAILED"

        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(request.callbackUrl, json=payload, headers={"X-Internal-Token": settings.internal_token})
            response.raise_for_status()


workflow_service = WorkflowService()
