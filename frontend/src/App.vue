<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">O</div>
        <div>
          <strong>OmniAgent</strong>
          <span>Agent Command Center</span>
        </div>
      </div>
      <p class="sidebar-section-label">Workspace</p>
      <nav class="nav">
        <RouterLink to="/"><LayoutDashboard :size="18" />总览</RouterLink>
        <RouterLink to="/chat"><Bot :size="18" />智能体</RouterLink>
        <RouterLink to="/knowledge"><LibraryBig :size="18" />知识库</RouterLink>
        <RouterLink to="/tools"><Wrench :size="18" />工具技能</RouterLink>
        <RouterLink to="/models"><SlidersHorizontal :size="18" />模型</RouterLink>
        <RouterLink to="/stats"><ChartNoAxesColumn :size="18" />统计</RouterLink>
        <RouterLink to="/feedback"><MessageSquareText :size="18" />反馈</RouterLink>
        <RouterLink to="/admin"><Shield :size="18" />管理员</RouterLink>
      </nav>
      <div class="sidebar-footer">
        <strong>Operational Layer</strong>
        <span>Planner、RAG、Vision、Tool、Critic 与 Answer 汇聚为可观测任务链。</span>
      </div>
    </aside>
    <main class="main">
      <div class="workspace">
        <header class="topbar">
          <div class="topbar-card">
            <div>
              <p class="page-kicker">{{ meta.kicker }}</p>
              <h1>{{ meta.title }}</h1>
              <p class="subtitle">{{ meta.subtitle }}</p>
              <div class="command-strip">
                <span v-for="item in meta.tags" :key="item" class="command-pill">{{ item }}</span>
              </div>
            </div>
          </div>
          <div class="topbar-card is-compact">
            <div class="title-row">
              <div>
                <span class="muted">当前模式</span>
                <strong style="display:block; margin-top: 5px;">通用智能体任务舱</strong>
              </div>
              <span class="status-badge is-success">ONLINE</span>
            </div>
            <div class="title-row">
              <div class="topbar-meta">
                <span class="command-pill">{{ deviceSummary }}</span>
                <span class="command-pill">{{ quotaSummary }}</span>
              </div>
              <el-button :icon="RefreshCcw" @click="reloadQuota">刷新额度</el-button>
            </div>
          </div>
        </header>
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, ChartNoAxesColumn, LayoutDashboard, LibraryBig, MessageSquareText, RefreshCcw, Shield, SlidersHorizontal, Wrench } from 'lucide-vue-next'
import { deviceApi } from './api/http'

const route = useRoute()
const quota = ref(null)
const meta = computed(() => {
  const map = {
    '/': {
      kicker: 'Command Overview',
      title: 'OmniAgent 总览',
      subtitle: '查看最近会话、知识库状态、模型接入和 Token 消耗，快速进入一次智能体任务。',
      tags: ['AgentOps', 'LLMOps', 'RAG']
    },
    '/chat': {
      kicker: 'Agent Mission Control',
      title: '智能体任务舱',
      subtitle: '提问、上传文档或图片，选择能力模式，查看多 Agent 执行图谱和带引用回答。',
      tags: ['Planner', 'Trace', 'Answer']
    },
    '/knowledge': {
      kicker: 'RAG Workspace',
      title: '知识库',
      subtitle: '上传文档并入库，供 RAG Agent 在回答时检索和引用。',
      tags: ['Ingestion', 'Chunks', 'Citation']
    },
    '/tools': {
      kicker: 'Tool Registry',
      title: '工具与技能',
      subtitle: '管理 MCP 工具和本地 Skill 能力，供 Tool Agent 在任务中调用。',
      tags: ['MCP', 'HTTP', 'Skills']
    },
    '/models': {
      kicker: 'Model Routing',
      title: '模型配置',
      subtitle: '配置 DeepSeek、通义千问、OpenAI 等模型供应商和默认模型。',
      tags: ['Provider', 'Model', 'Endpoint']
    },
    '/stats': {
      kicker: 'Token Intelligence',
      title: '运行统计',
      subtitle: '按会话回看 Agent、模型、Token 和耗时，支撑 LLMOps 分析。',
      tags: ['Token', 'Latency', 'Usage']
    },
    '/feedback': {
      kicker: 'Human Feedback',
      title: '用户反馈',
      subtitle: '记录用户评分和改进意见，为 Prompt、RAG 和 Agent 输出提供反馈闭环。',
      tags: ['Score', 'Review', 'Loop']
    },
    '/admin': {
      kicker: 'Quota Control',
      title: '管理员控制台',
      subtitle: '查看设备访问情况，调整白名单和每日 Token 额度，适配面试演示使用场景。',
      tags: ['Device', 'Quota', 'Whitelist']
    }
  }
  return map[route.path] || {
    kicker: 'Agent Workspace',
    title: '智能体工作台',
    subtitle: '启动智能体任务并查看执行过程。',
    tags: ['Agent', 'Workflow']
  }
})

const deviceSummary = computed(() => {
  if (!quota.value) return '设备识别中'
  return `设备 ${quota.value.shortDeviceId}`
})

const quotaSummary = computed(() => {
  if (!quota.value) return '额度加载中'
  if (quota.value.unlimitedQuota) return '白名单不限额'
  return `今日 ${quota.value.usedToday} / ${quota.value.dailyLimit || 0}`
})

async function reloadQuota() {
  const res = await deviceApi.quota()
  quota.value = res.data
}

onMounted(async () => {
  await reloadQuota()
})
</script>
