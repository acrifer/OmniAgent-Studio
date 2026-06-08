from __future__ import annotations

import asyncio
import json
import operator
import time
import uuid
from dataclasses import dataclass, field
from typing import Annotated, Any, TypedDict

import httpx
from langgraph.graph import END, START, StateGraph

from app.core.config import settings
from app.llm.llm_client import llm_client
from app.observability.langfuse_client import trace_recorder
from app.schemas.workflow import (
    AgentOutput,
    KnowledgeIngestRequest,
    KnowledgeIngestResponse,
    KnowledgeRetrieveRequest,
    KnowledgeRetrieveResponse,
    McpCallRequest,
    StartOmniAgentRunRequest,
    TaskStatus,
)


AGENTS = ["PLANNER", "SEARCH", "READER", "RAG", "VISION", "TOOL", "CRITIC", "ANSWER"]
PROGRESS = {
    "PLANNER": (8, 18),
    "SEARCH": (22, 34),
    "READER": (36, 48),
    "RAG": (50, 60),
    "VISION": (62, 70),
    "TOOL": (72, 78),
    "CRITIC": (82, 88),
    "ANSWER": (92, 98),
}


class OmniState(TypedDict, total=False):
    ai_task_id: str
    request: StartOmniAgentRunRequest
    trace_id: str
    completed_agents: Annotated[list[str], operator.add]
    planner_output: dict[str, Any]
    search_output: dict[str, Any]
    reader_output: dict[str, Any]
    rag_output: dict[str, Any]
    vision_output: dict[str, Any]
    tool_output: dict[str, Any]
    critic_output: dict[str, Any]
    answer_output: dict[str, Any]


@dataclass
class RuntimeTask:
    ai_task_id: str
    status: str = "RUNNING"
    progress: int = 0
    nodes: dict[str, str] = field(default_factory=dict)


class OmniAgentService:
    def __init__(self) -> None:
        self.tasks: dict[str, RuntimeTask] = {}
        self.knowledge_chunks: dict[int, list[dict[str, Any]]] = {}
        self.graph = self._build_graph()

    async def start(self, request: StartOmniAgentRunRequest) -> str:
        ai_task_id = str(uuid.uuid4())
        self.tasks[ai_task_id] = RuntimeTask(ai_task_id=ai_task_id, nodes={agent: "PENDING" for agent in AGENTS})
        asyncio.create_task(self._run(ai_task_id, request))
        return ai_task_id

    def status(self, ai_task_id: str) -> TaskStatus:
        task = self.tasks.get(ai_task_id)
        if task is None:
            return TaskStatus(aiTaskId=ai_task_id, status="NOT_FOUND", progress=0, nodes={})
        return TaskStatus(aiTaskId=ai_task_id, status=task.status, progress=task.progress, nodes=task.nodes)

    def ingest(self, request: KnowledgeIngestRequest) -> KnowledgeIngestResponse:
        text = (request.text or "").strip()
        if not text:
            return KnowledgeIngestResponse(status="FAILED", chunkCount=0, message="知识库文档没有可入库的文本。")
        chunks = []
        size = 1200
        for index in range(0, len(text), size):
            chunk = text[index:index + size].strip()
            if chunk:
                chunks.append({"documentId": request.documentId, "chunkIndex": len(chunks), "content": chunk})
        self.knowledge_chunks.setdefault(request.knowledgeBaseId, [])
        self.knowledge_chunks[request.knowledgeBaseId] = [
            chunk for chunk in self.knowledge_chunks[request.knowledgeBaseId]
            if chunk.get("documentId") != request.documentId
        ] + chunks
        return KnowledgeIngestResponse(status="READY", chunkCount=len(chunks), message="知识库文本已完成切片入库。")

    def retrieve(self, request: KnowledgeRetrieveRequest) -> KnowledgeRetrieveResponse:
        chunks = self._retrieve_chunks(request.knowledgeBaseId, request.query, request.topK)
        return KnowledgeRetrieveResponse(chunks=chunks)

    async def mcp_call(self, request: McpCallRequest) -> dict[str, Any]:
        if not request.endpoint:
            raise RuntimeError("MCP 工具 endpoint 未配置，无法调用工具。")
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(request.endpoint, json={"toolName": request.toolName, "input": request.inputJson})
            response.raise_for_status()
            return response.json()

    def _build_graph(self):
        graph = StateGraph(OmniState)
        for agent in AGENTS:
            graph.add_node(agent, self._agent_node(agent))
        graph.add_edge(START, "PLANNER")
        graph.add_edge("PLANNER", "SEARCH")
        graph.add_edge("SEARCH", "READER")
        graph.add_edge("READER", "RAG")
        graph.add_edge("RAG", "VISION")
        graph.add_edge("VISION", "TOOL")
        graph.add_edge("TOOL", "CRITIC")
        graph.add_edge("CRITIC", "ANSWER")
        graph.add_edge("ANSWER", END)
        return graph.compile()

    async def _run(self, ai_task_id: str, request: StartOmniAgentRunRequest) -> None:
        runtime = self.tasks[ai_task_id]
        try:
            metadata = {
                "run_id": request.runId,
                "conversation_id": request.conversationId,
                "mode": request.mode,
                "langfuse_enabled": trace_recorder.enabled,
            }
            with trace_recorder.trace("omniagent-run", metadata) as trace_id:
                await self._event(request, ai_task_id, "TASK_STARTED", None, "RUNNING", 3, langfuse_trace_id=trace_id)
                await self.graph.ainvoke({
                    "ai_task_id": ai_task_id,
                    "request": request,
                    "trace_id": trace_id,
                    "completed_agents": [],
                })
                runtime.status = "SUCCESS"
                runtime.progress = 100
                await self._event(request, ai_task_id, "TASK_FINISHED", None, "SUCCESS", 100, langfuse_trace_id=trace_id)
        except Exception as exc:
            runtime.status = "FAILED"
            runtime.progress = 100
            await self._event(request, ai_task_id, "TASK_FAILED", None, "FAILED", 100, error_message=str(exc))

    def _agent_node(self, agent: str):
        async def node(state: OmniState) -> dict[str, Any]:
            request = state["request"]
            ai_task_id = state["ai_task_id"]
            trace_id = state["trace_id"]
            runtime = self.tasks[ai_task_id]
            start_progress, success_progress = PROGRESS[agent]
            if not self._should_run(agent, state):
                runtime.nodes[agent] = "SKIPPED"
                await self._event(request, ai_task_id, "NODE_SKIPPED", agent, "SKIPPED", start_progress, langfuse_trace_id=trace_id)
                return {"completed_agents": []}

            runtime.nodes[agent] = "RUNNING"
            await self._event(request, ai_task_id, "NODE_STARTED", agent, "RUNNING", start_progress, langfuse_trace_id=trace_id)
            prompt = self._prompt(agent, state)
            try:
                output = await self._execute_agent(agent, state, prompt, trace_id)
                runtime.nodes[agent] = "SUCCESS"
                runtime.progress = success_progress
                await self._event(
                    request,
                    ai_task_id,
                    "NODE_SUCCESS",
                    agent,
                    "SUCCESS",
                    success_progress,
                    input_json=json.dumps(self._input_payload(agent, state), ensure_ascii=False),
                    output_json=json.dumps(output.content_json, ensure_ascii=False),
                    content_markdown=output.content_markdown,
                    citations_json=json.dumps(output.content_json.get("citations", []), ensure_ascii=False),
                    model_name=request.modelName,
                    prompt_version="omni-v1",
                    langfuse_trace_id=trace_id,
                    trace_observation_id=f"{trace_id}-{agent.lower()}",
                    prompt_tokens=output.prompt_tokens,
                    completion_tokens=output.completion_tokens,
                    latency_ms=output.latency_ms,
                )
                return {self._output_key(agent): output.content_json, "completed_agents": [agent]}
            except Exception as exc:
                runtime.nodes[agent] = "FAILED"
                await self._event(request, ai_task_id, "NODE_FAILED", agent, "FAILED", start_progress, error_message=str(exc), langfuse_trace_id=trace_id)
                if self._is_required(agent, request):
                    raise
                return {self._output_key(agent): {"status": "FAILED", "error": str(exc)}, "completed_agents": [agent]}

        return node

    async def _execute_agent(self, agent: str, state: OmniState, prompt: str, trace_id: str) -> AgentOutput:
        request = state["request"]
        if agent == "SEARCH":
            return await self._search_agent(state)
        if agent == "RAG":
            return await self._rag_agent(state)
        if agent == "VISION":
            raise RuntimeError("当前未配置视觉模型或图片二进制传输，无法执行图片理解。请配置 Qwen-VL/OpenAI Vision 并上传可访问图片。")
        if agent == "TOOL":
            raise RuntimeError("当前会话未配置 MCP 工具 endpoint，无法执行工具调用。")
        system = "你是 OmniAgent Studio 的内部智能体。只输出合法 JSON，不要代码块，不要额外解释。"
        with trace_recorder.generation(trace_id, agent, prompt, request.modelName) as observation_id:
            output = await llm_client.generate_json(system, prompt, request.modelName)
            output.agent_type = agent
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
            output.content_markdown = self._markdown(agent, output.content_json)
            return output

    async def _search_agent(self, state: OmniState) -> AgentOutput:
        if not settings.tavily_api_key:
            raise RuntimeError("TAVILY_API_KEY 未配置，无法执行联网搜索。")
        request = state["request"]
        start = time.perf_counter()
        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(
                "https://api.tavily.com/search",
                json={"api_key": settings.tavily_api_key, "query": request.question, "search_depth": "basic", "max_results": 5},
            )
            response.raise_for_status()
            data = response.json()
        results = [
            {"title": item.get("title"), "url": item.get("url"), "content": item.get("content", "")[:500]}
            for item in data.get("results", [])
        ]
        payload = {"query": request.question, "results": results, "citations": [{"title": r["title"], "url": r["url"]} for r in results if r.get("url")]}
        return AgentOutput(agent_type="SEARCH", content_json=payload, content_markdown=self._markdown("SEARCH", payload), prompt_tokens=0, completion_tokens=0, latency_ms=int((time.perf_counter() - start) * 1000))

    async def _rag_agent(self, state: OmniState) -> AgentOutput:
        request = state["request"]
        if request.knowledgeBaseId is None:
            raise RuntimeError("未选择知识库，无法执行知识库问答。")
        chunks = self._retrieve_chunks(request.knowledgeBaseId, request.question, 5)
        if not chunks:
            raise RuntimeError("知识库没有可检索内容，请先上传并完成入库。")
        payload = {"query": request.question, "chunks": chunks, "citations": [{"documentId": c["documentId"], "chunkIndex": c["chunkIndex"]} for c in chunks]}
        return AgentOutput(agent_type="RAG", content_json=payload, content_markdown=self._markdown("RAG", payload), prompt_tokens=0, completion_tokens=0, latency_ms=1)

    def _retrieve_chunks(self, knowledge_base_id: int, query: str, top_k: int) -> list[dict[str, Any]]:
        terms = {part.lower() for part in query.replace("，", " ").replace("。", " ").split() if len(part) > 1}
        scored = []
        for chunk in self.knowledge_chunks.get(knowledge_base_id, []):
            content = chunk["content"]
            lowered = content.lower()
            score = sum(1 for term in terms if term in lowered)
            if score or not terms:
                scored.append({**chunk, "score": score, "content": content[:900]})
        scored.sort(key=lambda item: item["score"], reverse=True)
        return scored[:top_k]

    def _should_run(self, agent: str, state: OmniState) -> bool:
        if agent in {"PLANNER", "CRITIC", "ANSWER"}:
            return True
        request = state["request"]
        mode = (request.mode or "AUTO").upper()
        has_docs = bool(request.documents)
        has_images = any(file.get("parseStatus") == "IMAGE" for file in request.files)
        if agent == "SEARCH":
            return mode == "SEARCH"
        if agent == "READER":
            return mode in {"AUTO", "DOCUMENT", "READING"} and has_docs
        if agent == "RAG":
            return mode in {"RAG", "KNOWLEDGE"} or (mode == "AUTO" and request.knowledgeBaseId is not None)
        if agent == "VISION":
            return mode in {"IMAGE", "VISION"} or (mode == "AUTO" and has_images)
        if agent == "TOOL":
            return mode == "TOOL"
        return False

    def _is_required(self, agent: str, request: StartOmniAgentRunRequest) -> bool:
        mode = (request.mode or "AUTO").upper()
        return agent in {"PLANNER", "CRITIC", "ANSWER"} or (agent == "SEARCH" and mode == "SEARCH") or (agent == "RAG" and mode in {"RAG", "KNOWLEDGE"}) or (agent == "VISION" and mode in {"IMAGE", "VISION"}) or (agent == "TOOL" and mode == "TOOL")

    def _prompt(self, agent: str, state: OmniState) -> str:
        request = state["request"]
        context = json.dumps(self._input_payload(agent, state), ensure_ascii=False)[:16000]
        tasks = {
            "PLANNER": "理解用户任务，判断需要哪些能力，输出 intent、required_agents、plan_steps、risks。",
            "READER": "阅读用户上传文档内容，输出 key_points、entities、constraints、open_questions、citations。",
            "CRITIC": "检查已有证据和中间结果的一致性，输出 factual_issues、missing_evidence、confidence、fix_suggestions。",
            "ANSWER": "生成最终回答，必须输出 answerMarkdown、citations、usedTools、confidence、followUpQuestions。",
        }
        return f"""
用户问题：{request.question}
会话模式：{request.mode}
上下文 JSON：{context}

当前 Agent：{agent}
任务：{tasks.get(agent, "根据上下文执行对应能力并输出结构化 JSON。")}

输出要求：
- 只输出合法 JSON。
- 不要编造不存在的引用；引用只能来自 search/rag/reader 已有上下文。
- 如果信息不足，请在 JSON 中说明缺口，不要假装已完成。
"""

    def _input_payload(self, agent: str, state: OmniState) -> dict[str, Any]:
        request = state["request"]
        payload: dict[str, Any] = {
            "question": request.question,
            "mode": request.mode,
            "knowledgeBaseId": request.knowledgeBaseId,
            "documents": [doc[:3000] for doc in request.documents],
            "files": request.files,
        }
        for completed in state.get("completed_agents", []):
            key = self._output_key(completed)
            if state.get(key) is not None:
                payload[completed] = state[key]
        return payload

    def _markdown(self, agent: str, payload: dict[str, Any]) -> str:
        if agent == "ANSWER" and payload.get("answerMarkdown"):
            return str(payload["answerMarkdown"])
        lines = [f"## {agent} Agent", ""]
        for key, value in payload.items():
            lines.append(f"### {key}")
            if isinstance(value, (dict, list)):
                lines.append("```json")
                lines.append(json.dumps(value, ensure_ascii=False, indent=2))
                lines.append("```")
            else:
                lines.append(str(value))
            lines.append("")
        return "\n".join(lines)

    def _output_key(self, agent: str) -> str:
        return f"{agent.lower()}_output"

    async def _event(self, request: StartOmniAgentRunRequest, ai_task_id: str, event_type: str, agent_type: str | None, status: str, progress: int, **kwargs) -> None:
        runtime = self.tasks.get(ai_task_id)
        if runtime:
            runtime.progress = max(runtime.progress, progress)
        payload = {
            "aiTaskId": ai_task_id,
            "runId": request.runId,
            "conversationId": request.conversationId,
            "messageId": request.messageId,
            "eventType": event_type,
            "agentType": agent_type,
            "status": status,
            "progress": progress,
            **kwargs,
        }
        headers = {"X-Internal-Token": settings.internal_token}
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                await client.post(str(request.callbackUrl), headers=headers, json=payload)
        except Exception:
            pass


omni_agent_service = OmniAgentService()
