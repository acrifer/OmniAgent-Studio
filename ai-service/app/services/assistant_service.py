from __future__ import annotations

import httpx

from app.core.config import settings
from app.llm.llm_client import llm_client
from app.schemas.workflow import RequirementDraftRequest, RequirementDraftResponse


class AssistantService:
    async def generate_requirement_draft(self, request: RequirementDraftRequest) -> RequirementDraftResponse:
        if not request.modelName:
            raise RuntimeError("请配置 DeepSeek 模型名称。")
        if not settings.deepseek_api_key:
            raise RuntimeError("DEEPSEEK_API_KEY 未配置，无法生成需求草案。")
        return await self._deepseek_requirement_draft(request)

    async def _deepseek_requirement_draft(self, request: RequirementDraftRequest) -> RequirementDraftResponse:
        prompt = f"""
你是软件工程毕业设计指导老师和 AI 应用产品经理。请根据项目标题生成可直接编辑的需求草案和建议大纲。

项目标题：{request.projectTitle}
项目补充描述：{request.projectDescription or ""}
技术栈：{request.techStack or "Vue 3 + Element Plus, Spring Boot, MySQL, Redis, FastAPI, LangGraph, Langfuse"}

要求：
- 输出必须是合法 JSON，不要 Markdown，不要代码块。
- requirementText 使用中文完整段落，可直接粘贴到需求编辑器。
- outline 是 5-8 个建议章节标题。
- suggestedModules 是 6-10 个功能模块名。
- techRisks 是 3-6 个真实可落地的技术风险。

JSON 格式：
{{
  "requirementText": "",
  "outline": [],
  "suggestedModules": [],
  "techRisks": []
}}
"""
        try:
            async with httpx.AsyncClient(timeout=settings.deepseek_timeout_seconds) as client:
                response = await client.post(
                    f"{settings.deepseek_base_url.rstrip('/')}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {settings.deepseek_api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": request.modelName,
                        "messages": [
                            {"role": "system", "content": "你输出稳定、可解析、适合软件工程本科毕设的 JSON。"},
                            {"role": "user", "content": prompt},
                        ],
                        "temperature": 0.3,
                        "stream": False,
                    },
                )
                response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            detail = llm_client._response_error_detail(exc.response)
            raise RuntimeError(f"DeepSeek API 调用失败：HTTP {exc.response.status_code}，{detail}") from exc
        except httpx.RequestError as exc:
            raise RuntimeError(f"DeepSeek API 请求失败：{exc}") from exc
        content = response.json()["choices"][0]["message"]["content"]
        parsed = llm_client._parse_json_content(content)
        if "requirementText" not in parsed:
            parsed = {
                "requirementText": parsed.get("raw_output", content),
                "outline": ["需求分析", "功能设计", "系统架构", "测试方案", "风险优化"],
                "suggestedModules": [],
                "techRisks": ["模型输出格式不稳定，已使用原始结果兜底。"],
            }
        return RequirementDraftResponse(
            requirementText=str(parsed.get("requirementText", "")),
            outline=[str(item) for item in parsed.get("outline", [])],
            suggestedModules=[str(item) for item in parsed.get("suggestedModules", [])],
            techRisks=[str(item) for item in parsed.get("techRisks", [])],
        )


assistant_service = AssistantService()
