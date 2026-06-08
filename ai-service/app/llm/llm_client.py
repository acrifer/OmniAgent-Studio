from __future__ import annotations

import json
import re
import time
from typing import Any

import httpx

from app.core.config import settings
from app.schemas.workflow import AgentOutput


class LlmClient:
    async def generate_json(self, system: str, prompt: str, model_name: str = "deepseek-v4-flash", temperature: float = 0.2) -> AgentOutput:
        return await self._chat_json("OMNI", system, prompt, model_name or settings.default_model, temperature)

    async def _chat_json(self, agent_type: str, system: str, prompt: str, model_name: str, temperature: float) -> AgentOutput:
        provider = self._provider_for_model(model_name)
        api_key, base_url = self._provider_config(provider)
        if not api_key:
            raise RuntimeError(f"{provider.upper()} API Key 未配置，无法调用模型 {model_name}。")
        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=settings.deepseek_timeout_seconds) as client:
                response = await client.post(
                    f"{base_url.rstrip('/')}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": model_name,
                        "messages": [
                            {"role": "system", "content": system},
                            {"role": "user", "content": prompt},
                        ],
                        "temperature": temperature,
                        "stream": False,
                    },
                )
                response.raise_for_status()
                data = response.json()
        except httpx.HTTPStatusError as exc:
            detail = self._response_error_detail(exc.response)
            raise RuntimeError(f"{provider.upper()} API 调用失败：HTTP {exc.response.status_code}，{detail}") from exc
        except httpx.RequestError as exc:
            raise RuntimeError(f"{provider.upper()} API 请求失败：{exc}") from exc

        content = data["choices"][0]["message"]["content"]
        parsed = self._parse_json_content(content)
        usage = data.get("usage", {})
        return AgentOutput(
            agent_type=agent_type,
            content_json=parsed,
            content_markdown=self._to_markdown(agent_type, parsed),
            prompt_tokens=int(usage.get("prompt_tokens", 0) or 0),
            completion_tokens=int(usage.get("completion_tokens", 0) or 0),
            latency_ms=int((time.perf_counter() - start) * 1000),
        )

    async def generate_agent_output(self, agent_type: str, project_name: str, requirement_text: str, context: dict, model_name: str = "deepseek-v4-flash", prompt: str | None = None) -> AgentOutput:
        if not model_name:
            raise RuntimeError("请配置 DeepSeek 模型名称。")
        return await self._deepseek(agent_type, project_name, requirement_text, context, model_name, prompt)

    async def _deepseek(self, agent_type: str, project_name: str, requirement_text: str, context: dict, model_name: str, prompt: str | None) -> AgentOutput:
        if not settings.deepseek_api_key:
            raise RuntimeError("DEEPSEEK_API_KEY 未配置，无法调用 DeepSeek API。")

        prompt = prompt or self.build_prompt(agent_type, project_name, requirement_text, context)
        start = time.perf_counter()
        try:
            async with httpx.AsyncClient(timeout=settings.deepseek_timeout_seconds) as client:
                response = await client.post(
                    f"{settings.deepseek_base_url.rstrip('/')}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {settings.deepseek_api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": model_name,
                        "messages": [
                            {"role": "system", "content": "你是 OmniAgent Studio 中的专业 Agent。请严格按用户要求输出可解析 JSON。"},
                            {"role": "user", "content": prompt},
                        ],
                        "temperature": 0.2,
                        "stream": False,
                    },
                )
                response.raise_for_status()
                data = response.json()
        except httpx.HTTPStatusError as exc:
            detail = self._response_error_detail(exc.response)
            raise RuntimeError(f"DeepSeek API 调用失败：HTTP {exc.response.status_code}，{detail}") from exc
        except httpx.RequestError as exc:
            raise RuntimeError(f"DeepSeek API 请求失败：{exc}") from exc

        content = data["choices"][0]["message"]["content"]
        parsed = self._parse_json_content(content)
        usage = data.get("usage", {})
        return AgentOutput(
            agent_type=agent_type,
            content_json=parsed,
            content_markdown=self._to_markdown(agent_type, parsed),
            prompt_tokens=int(usage.get("prompt_tokens", 0) or 0),
            completion_tokens=int(usage.get("completion_tokens", 0) or 0),
            latency_ms=int((time.perf_counter() - start) * 1000),
        )

    def _parse_json_content(self, content: str) -> dict[str, Any]:
        text = content.strip()
        fenced = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL | re.IGNORECASE)
        if fenced:
            text = fenced.group(1).strip()
        try:
            value = json.loads(text)
            return value if isinstance(value, dict) else {"items": value}
        except json.JSONDecodeError:
            return {"raw_output": content}

    def build_prompt(self, agent_type: str, project_name: str, requirement_text: str, context: dict) -> str:
        tasks = {
            "PM": "输出 project_goal、target_users、functional_modules、user_stories、out_of_scope、risks。",
            "ARCHITECT": "输出 components、call_chain、async_tasks、deployment、architecture_risks。",
            "FRONTEND": "输出 routes、pages、components、api_dependencies、ui_risks。",
            "BACKEND": "输出 modules、controllers、services、entities、fastapi_client、auth_design、error_handling。",
            "TEST": "输出 test_suites、acceptance_criteria、high_risk_cases。",
            "SECURITY": "输出 security_risks、auth_checks、file_upload_checks、prompt_injection_checks。",
            "SUMMARY": "整合所有 Agent 输出，输出 report_title、markdown_report、structured_summary、conflict_notes、next_steps。",
        }
        context_text = json.dumps(context, ensure_ascii=False)[:12000]
        return f"""
项目名称：{project_name}
需求描述：{requirement_text}
已知上下文 JSON：{context_text}

当前 Agent：{agent_type}
任务：{tasks[agent_type]}

通用约束：
- 输出必须是合法 JSON，不要 Markdown，不要代码块。
- 内容必须围绕当前项目，不要编造无法验证的事实。
- 方案要适合本科毕业设计和简历项目，不要过度企业级。
"""

    def _to_markdown(self, agent_type: str, payload: dict) -> str:
        if agent_type == "SUMMARY" and payload.get("markdown_report"):
            return str(payload["markdown_report"])
        lines = [f"## {agent_type} Agent 输出", ""]
        for key, value in payload.items():
            lines.append(f"### {key}")
            if isinstance(value, (list, dict)):
                lines.append("```json")
                lines.append(json.dumps(value, ensure_ascii=False, indent=2))
                lines.append("```")
            else:
                lines.append(str(value))
            lines.append("")
        return "\n".join(lines)

    def _response_error_detail(self, response: httpx.Response) -> str:
        text = response.text[:800]
        try:
            data = response.json()
            message = data.get("error", {}).get("message") or data.get("message") or text
            return str(message)[:800]
        except ValueError:
            return text or "无响应内容"

    def _provider_for_model(self, model_name: str) -> str:
        lowered = (model_name or "").lower()
        if lowered.startswith("qwen") or "dashscope" in lowered:
            return "qwen"
        if lowered.startswith("gpt") or lowered.startswith("o1") or lowered.startswith("o3") or lowered.startswith("o4"):
            return "openai"
        return "deepseek"

    def _provider_config(self, provider: str) -> tuple[str | None, str]:
        if provider == "qwen":
            return settings.qwen_api_key, settings.qwen_base_url
        if provider == "openai":
            return settings.openai_api_key, settings.openai_base_url
        return settings.deepseek_api_key, settings.deepseek_base_url


llm_client = LlmClient()
