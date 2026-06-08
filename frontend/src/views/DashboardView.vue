<template>
  <section class="section-stack">
    <div class="console-grid">
      <MetricCard class="span-4" label="会话总数" :value="conversations.length" hint="当前账号智能体会话" trend="Conversation archive" :icon="Bot" tone="cyan" />
      <MetricCard class="span-4" label="最近运行" :value="latestStatus" hint="Agent Run 状态" trend="Latest workflow status" :icon="Workflow" tone="green" />
      <MetricCard class="span-4" label="累计 Token" :value="totalTokens" hint="最近会话调用消耗" trend="Recent cost surface" :icon="Sparkles" tone="amber" />
    </div>

    <div class="console-grid">
      <ActionPanel class="span-8" eyebrow="Recent Conversations" title="最近会话" description="从这里快速进入智能体对话，继续追问、上传文件或查看执行过程。">
        <template #action>
          <RouterLink to="/chat"><el-button type="primary">进入智能体</el-button></RouterLink>
        </template>
        <EmptyState v-if="!conversations.length" :icon="Bot" title="暂无会话" description="创建会话后会显示在这里。" compact />
        <div v-else class="inline-list">
          <RouterLink v-for="item in conversations.slice(0, 5)" :key="item.id" to="/chat" class="surface-strip">
            <div class="title-row">
              <div>
                <strong>{{ item.title }}</strong>
                <p class="muted" style="margin: 6px 0 0;">{{ item.mode }} · {{ item.modelName }}</p>
              </div>
              <span class="status-badge">{{ item.mode }}</span>
            </div>
          </RouterLink>
        </div>
      </ActionPanel>

      <ActionPanel class="span-4" eyebrow="Agent Capability" title="能力概览" description="平台面向真实任务，不暴露复杂编排器，只让用户选择要启用的能力。">
        <div class="inline-list">
          <div class="surface-strip"><strong>最新运行</strong><p class="muted">{{ latestTaskText }}</p></div>
          <div class="surface-strip"><strong>知识库</strong><p class="muted">{{ knowledgeText }}</p></div>
          <div class="surface-strip"><strong>多 Agent</strong><p class="muted">Planner 规划，Reader/RAG/Search/Vision/Tool 按需执行，Critic 校验，Answer 汇总。</p></div>
          <div class="surface-strip"><strong>可观测</strong><p class="muted">步骤、Token、耗时、Trace、评分都进入业务闭环。</p></div>
        </div>
      </ActionPanel>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bot, Sparkles, Workflow } from 'lucide-vue-next'
import { conversationApi, knowledgeApi, omniStatsApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import MetricCard from '../components/MetricCard.vue'

const conversations = ref([])
const latestConversationDetail = ref(null)
const knowledgeBases = ref([])
const totalTokens = ref(0)
const latestStatus = computed(() => latestConversationDetail.value?.latestRun?.status || 'N/A')
const latestTaskText = computed(() => {
  const task = latestConversationDetail.value?.latestRun
  if (!task) return '暂无运行记录'
  return `${task.status} · ${task.mode} · ${task.modelName}`
})
const knowledgeText = computed(() => knowledgeBases.value.length ? `${knowledgeBases.value.length} 个知识库可用` : '暂无知识库')

onMounted(async () => {
  const [conversationRes, kbRes] = await Promise.all([conversationApi.list(), knowledgeApi.list()])
  conversations.value = conversationRes.data || []
  knowledgeBases.value = kbRes.data || []
  if (conversations.value.length) {
    const [detailRes, statsRes] = await Promise.all([
      conversationApi.detail(conversations.value[0].id),
      omniStatsApi.tokens(conversations.value[0].id)
    ])
    latestConversationDetail.value = detailRes.data
    totalTokens.value = statsRes.data.totalTokens || 0
  }
})
</script>
