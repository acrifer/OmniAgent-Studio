from contextlib import contextmanager
from typing import Any

from app.core.config import settings


class TraceRecorder:
    def __init__(self) -> None:
        self.client = None
        self.enabled = False
        self.generations: dict[str, Any] = {}
        if settings.langfuse_enabled and settings.langfuse_public_key and settings.langfuse_secret_key:
            try:
                from langfuse import Langfuse

                self.client = Langfuse(
                    public_key=settings.langfuse_public_key,
                    secret_key=settings.langfuse_secret_key,
                    host=settings.langfuse_host,
                )
                self.enabled = True
            except Exception:
                self.client = None
                self.enabled = False

    @contextmanager
    def trace(self, name: str, metadata: dict):
        trace_id = f"trace-{metadata.get('task_id')}"
        trace = None
        if self.enabled and self.client:
            trace = self.client.trace(name=name, metadata=metadata)
            trace_id = trace.id
        try:
            yield trace_id
        finally:
            if self.enabled and self.client:
                self.client.flush()

    @contextmanager
    def generation(self, trace_id: str, agent_type: str, prompt: str, model: str):
        observation_id = f"{trace_id}-{agent_type.lower()}"
        generation = None
        if self.enabled and self.client:
            generation = self.client.generation(
                trace_id=trace_id,
                name=f"{agent_type} Agent",
                model=model,
                input=prompt,
                metadata={"agentType": agent_type},
            )
            observation_id = generation.id
            self.generations[observation_id] = generation
        try:
            yield observation_id
        finally:
            if generation is not None:
                generation.end()
                self.generations.pop(observation_id, None)
                self.client.flush()

    def update_generation(self, observation_id: str, output: Any, usage: dict[str, int], latency_ms: int | None = None) -> None:
        if not self.enabled or not self.client or not observation_id:
            return
        try:
            generation = self.generations.get(observation_id)
            if generation is None:
                return
            generation.update(output=output, usage=usage, metadata={"latencyMs": latency_ms})
            self.client.flush()
        except Exception:
            pass

    def score(self, trace_id: str | None, observation_id: str | None, name: str, value: float, comment: str | None, metadata: dict) -> bool:
        if not self.enabled or not self.client or not trace_id:
            return False
        try:
            self.client.score(
                trace_id=trace_id,
                observation_id=observation_id,
                name=name,
                value=value,
                comment=comment,
                metadata=metadata,
            )
            self.client.flush()
            return True
        except Exception:
            return False


trace_recorder = TraceRecorder()
