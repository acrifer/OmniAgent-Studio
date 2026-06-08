# OmniAgent Studio

基于多智能体协作与可视化的通用多模态智能体平台。用户可以直接提问、上传文档或图片、选择知识库和能力模式，系统由 Planner、Search、Reader、RAG、Vision、Tool、Critic、Answer 等内部 Agent 协作完成搜索、阅读、检索、工具调用、校验和最终回答。

项目定位是“可直接使用的通用智能体应用”，不是 Dify/Flowise 式低代码编排器。

## 核心能力

- 用户登录注册，测试账号：`demo / demo123456`
- 智能体会话：创建会话、发送问题、上传 txt/md/pdf/docx/png/jpg/jpeg/webp
- 能力模式：自动模式、联网搜索、知识库问答、文档阅读、图片理解、工具调用
- LangGraph 通用 Agent 工作流：Planner 规划，能力 Agent 按需执行，Critic 校验，Answer 汇总
- 可视化执行：Vue Flow 展示 Agent 图谱，步骤列表展示输入、输出、Token、耗时和错误
- 知识库 RAG：文档解析、文本切片入库、检索片段引用
- MCP/Skill：工具和本地技能配置入口
- 多模型接入：DeepSeek 默认，通义千问和 OpenAI 可选
- Langfuse：记录 Trace、Generation、Prompt、Token、耗时和评分
- 无本地假数据：缺少模型、搜索、视觉或工具配置时返回明确业务错误

## 技术栈

- 前端：Vue 3、Element Plus、Vite、lucide-vue-next、Vue Flow、ECharts
- 业务后端：Spring Boot 3、Spring Security、JPA、H2/MySQL、Apache Tika
- AI 服务：FastAPI、LangGraph、DeepSeek/OpenAI-compatible API、Langfuse
- 存储：MySQL/H2、Redis 可用于任务状态缓存，知识库第一版使用 AI 服务内存切片检索，可替换为 Qdrant/Chroma

## 本地运行

### 1. AI 服务

在 `ai-service/.env` 配置真实 Key，不要提交到仓库：

```env
DEEPSEEK_API_KEY=你的 DeepSeek Key
DEEPSEEK_BASE_URL=https://api.deepseek.com
TAVILY_API_KEY=可选，联网搜索需要
QWEN_API_KEY=可选，通义千问模型需要
OPENAI_API_KEY=可选，OpenAI 模型需要
LANGFUSE_ENABLED=false
```

启动：

```powershell
cd ai-service
C:\environment\python\python.exe -m pip install -r requirements.txt
C:\environment\python\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

### 2. Spring Boot 后端

```powershell
cd backend
mvn spring-boot:run
```

默认端口：`18080`。

### 3. 前端

```powershell
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

默认端口：`5173`，如端口被占用 Vite 会自动切换。

## 使用流程

1. 登录系统，进入“智能体”页面。
2. 创建或选择会话，输入问题。
3. 按需上传文档/图片，或在“知识库”页面上传资料并入库。
4. 选择自动、搜索、RAG、文档阅读、图片理解或工具调用模式。
5. 启动智能体，查看 Agent 图谱、步骤详情和最终回答。
6. 在“统计”页面查看 Token、模型和耗时，在“反馈”页面提交人工评分。

## 主要接口

- Spring Boot：`/api/conversations`、`/api/agent-runs`、`/api/knowledge-bases`、`/api/model-providers`、`/api/tools`、`/api/skills`
- FastAPI：`/ai/agent-runs/start`、`/ai/agent-runs/{aiTaskId}/status`、`/ai/knowledge/ingest`、`/ai/knowledge/retrieve`、`/ai/tools/mcp-call`、`/ai/health/models`

## 构建检查

```powershell
cd backend
mvn -q -DskipTests package

cd ..\ai-service
C:\environment\python\python.exe -m compileall app

cd ..\frontend
npm run build
```
