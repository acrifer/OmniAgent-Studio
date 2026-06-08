# OmniAgent Studio

OmniAgent Studio 是一个面向真实问答与资料处理场景的 AI 智能体工作台。用户可以登录后创建会话，上传文档或图片，选择知识库、联网搜索、图片理解或工具调用能力，由 Planner、Search、Reader、RAG、Vision、Tool、Critic、Answer 等 Agent 协作完成任务，并在前端查看执行图谱、步骤详情、Token 消耗和最终回答。

项目主题已经收敛为“多模态多智能体工作台”，不再保留旧的项目需求分析 / 报告生成模块作为产品主线。

## 核心能力

- 用户登录注册，测试账号：`demo / demo123456`
- 智能体会话：创建会话、发送问题、上传 `txt/md/pdf/docx/png/jpg/jpeg/webp`
- 多 Agent 编排：Planner 规划，能力 Agent 执行，Critic 校验，Answer 汇总
- 向量 RAG：文档解析、切片、Embedding、Qdrant 入库与相似度检索
- 图片理解：通过 OpenAI-compatible Vision 模型读取上传图片并输出结构化结果
- 工具调用：Tool Agent 调用用户配置的 HTTP/MCP endpoint，并记录输入、输出和耗时
- 内置演示工具：`/ai/tools/demo/echo` 可作为 Tool Agent 演示 endpoint
- 可视化执行：Vue Flow 展示 Agent 图谱，步骤列表展示输入、输出、Token、耗时和错误
- 多模型接入：DeepSeek 默认，通义千问和 OpenAI-compatible API 可选
- LLMOps：Langfuse 可选记录 Trace、Generation、Prompt、Token、耗时和评分

## 技术栈

- 前端：Vue 3、Element Plus、Vite、lucide-vue-next、Vue Flow、ECharts
- 业务后端：Spring Boot 3、Spring Security、JPA、H2/MySQL、Apache Tika、JWT
- AI 服务：FastAPI、LangGraph、DeepSeek/OpenAI-compatible API、Qdrant、Langfuse
- 工程化：Docker Compose、MySQL、Redis、Qdrant、Nginx

## 本地运行

### 1. AI 服务

复制 `ai-service/.env.example` 为 `ai-service/.env`，配置真实 Key：

```env
INTERNAL_TOKEN=devmind-internal-token
DEFAULT_MODEL=deepseek-v4-flash
DEEPSEEK_API_KEY=your-deepseek-api-key
DEEPSEEK_BASE_URL=https://api.deepseek.com
QWEN_API_KEY=
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
OPENAI_API_KEY=
OPENAI_BASE_URL=https://api.openai.com/v1
TAVILY_API_KEY=
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=omniagent_chunks
EMBEDDING_MODEL=text-embedding-v3
VISION_MODEL=qwen-vl-plus
LANGFUSE_ENABLED=false
```

启动 Qdrant 后再启动 AI 服务：

```powershell
docker run -p 6333:6333 qdrant/qdrant:v1.12.4

cd ai-service
C:\environment\python\python.exe -m pip install -r requirements.txt
C:\environment\python\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

### 2. Spring Boot 后端

```powershell
cd backend
mvn spring-boot:run
```

默认端口：`18080`。未配置 MySQL 时使用本地 H2 文件库。

### 3. 前端

```powershell
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

默认端口：`5173`。

## Docker Compose

完整工程化启动：

```powershell
copy ai-service\.env.example ai-service\.env
# 编辑 ai-service\.env，填入模型、Embedding、Vision、Tavily 等 Key
docker compose up --build
```

服务端口：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:18080`
- AI 服务：`http://localhost:8000`
- Qdrant：`http://localhost:6333`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

## 使用流程

1. 登录系统，进入“智能体”页面。
2. 创建或选择会话，输入问题。
3. 按需上传文档或图片，或在“知识库”页面上传资料并完成向量入库。
4. 选择自动、搜索、RAG、文档阅读、图片理解或工具调用模式。
5. 启动智能体，查看 Agent 图谱、步骤详情和最终回答。
6. 在“统计”页面查看 Token、模型和耗时，在“反馈”页面提交人工评分。

### Tool Agent 演示配置

在“工具技能”页面新增工具：

- 工具名称：`echo-tool`
- 类型：`HTTP`
- Endpoint：本地运行填 `http://localhost:8000/ai/tools/demo/echo`，Docker Compose 运行填 `http://ai-service:8000/ai/tools/demo/echo`
- 配置 JSON：`{}`

随后在“智能体”页面选择“工具调用”模式并提问，Tool Agent 会调用该 endpoint，并在步骤详情中展示工具输入输出。

## 主要接口

- Spring Boot：`/api/conversations`、`/api/agent-runs`、`/api/knowledge-bases`、`/api/model-providers`、`/api/tools`、`/api/skills`
- FastAPI：`/ai/agent-runs/start`、`/ai/agent-runs/{aiTaskId}/status`、`/ai/knowledge/ingest`、`/ai/knowledge/retrieve`、`/ai/tools/mcp-call`、`/ai/tools/demo/echo`、`/ai/health/models`

## 构建检查

```powershell
cd backend
mvn -q -DskipTests package

cd ..\ai-service
C:\environment\python\python.exe -m compileall app

cd ..\frontend
npm run build
```
