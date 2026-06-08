<template>
  <router-view v-if="$route.path === '/login'" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">O</div>
        <div>
          <strong>OmniAgent</strong>
          <span>Multimodal Agent Studio</span>
        </div>
      </div>
      <nav class="nav">
        <RouterLink to="/"><LayoutDashboard :size="18" />总览</RouterLink>
        <RouterLink to="/chat"><Bot :size="18" />智能体</RouterLink>
        <RouterLink to="/knowledge"><LibraryBig :size="18" />知识库</RouterLink>
        <RouterLink to="/tools"><Wrench :size="18" />工具技能</RouterLink>
        <RouterLink to="/models"><SlidersHorizontal :size="18" />模型</RouterLink>
        <RouterLink to="/stats"><ChartNoAxesColumn :size="18" />统计</RouterLink>
        <RouterLink to="/feedback"><MessageSquareText :size="18" />反馈</RouterLink>
      </nav>
    </aside>
    <main class="main">
      <header class="topbar">
        <div class="topbar-card">
          <div>
            <p class="page-kicker">Multimodal Multi-Agent Workspace</p>
            <h1>{{ title }}</h1>
            <p class="subtitle">{{ subtitle }}</p>
          </div>
        </div>
        <div class="topbar-card">
          <div>
            <span class="muted">当前模式</span>
            <strong style="display:block; margin-top: 4px;">通用智能体任务舱</strong>
          </div>
          <el-button :icon="LogOut" @click="logout">退出</el-button>
        </div>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bot, ChartNoAxesColumn, LayoutDashboard, LibraryBig, LogOut, MessageSquareText, SlidersHorizontal, Wrench } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const title = computed(() => {
  const map = {
    '/': 'OmniAgent 总览',
    '/chat': '智能体对话',
    '/knowledge': '知识库',
    '/tools': '工具与技能',
    '/models': '模型配置',
    '/stats': '运行统计',
    '/feedback': '用户反馈'
  }
  return map[route.path] || '智能体工作台'
})

const subtitle = computed(() => {
  const map = {
    '/': '查看最近会话、知识库状态、模型接入和 Token 消耗，快速进入一次智能体任务。',
    '/chat': '提问、上传文档或图片，选择能力模式，查看多 Agent 执行图谱和带引用回答。',
    '/knowledge': '上传文档并入库，供 RAG Agent 在回答时检索和引用。',
    '/tools': '管理 MCP 工具和本地 Skill 能力，供 Tool Agent 在任务中调用。',
    '/models': '配置 DeepSeek、通义千问、OpenAI 等模型供应商和默认模型。',
    '/stats': '按会话回看 Agent、模型、Token 和耗时，支撑 LLMOps 分析。',
    '/feedback': '记录用户评分和改进意见，为 Prompt、RAG 和 Agent 输出提供反馈闭环。'
  }
  return map[route.path] || '启动智能体任务并查看执行过程。'
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
