<template>
  <section class="config-layout">
    <ActionPanel eyebrow="Token Intelligence" title="Token 消耗查询" description="按会话查看每个 Agent 的模型调用、Token 和耗时。">
      <template #action>
        <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      </template>
      <el-form label-position="top">
        <el-form-item label="会话">
          <el-select v-model="conversationId" placeholder="选择会话" style="width: 100%">
            <el-option v-for="item in conversations" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <MetricCard label="总 Token" :value="stats.totalTokens || 0" hint="当前会话累计" trend="Token usage by selected conversation" :icon="ChartNoAxesColumn" tone="cyan" />
      <div class="chip-list" v-if="agentStats.length">
        <span v-for="item in agentStats" :key="item.agent">{{ item.agent }} · {{ item.total }}</span>
      </div>
    </ActionPanel>

    <ActionPanel eyebrow="Usage Records" title="调用明细">
      <EmptyState v-if="!(stats.records || []).length" :icon="ChartNoAxesColumn" title="暂无统计" description="完成一次 Agent 工作流后会产生 Token 记录。" compact />
      <div v-else class="table-shell">
        <el-table :data="stats.records || []">
          <el-table-column prop="agentType" label="Agent" width="120" />
          <el-table-column prop="modelName" label="模型" min-width="160" />
          <el-table-column prop="promptTokens" label="输入" width="100" />
          <el-table-column prop="completionTokens" label="输出" width="100" />
          <el-table-column prop="totalTokens" label="总 Token" width="120" />
          <el-table-column prop="latencyMs" label="耗时(ms)" width="120" />
        </el-table>
      </div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChartNoAxesColumn, Search } from 'lucide-vue-next'
import { conversationApi, omniStatsApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import MetricCard from '../components/MetricCard.vue'

const conversations = ref([])
const conversationId = ref(null)
const stats = ref({})
const agentStats = computed(() => {
  const grouped = {}
  for (const record of stats.value.records || []) {
    const agent = record.agentType || 'UNKNOWN'
    grouped[agent] = (grouped[agent] || 0) + (record.totalTokens || 0)
  }
  return Object.entries(grouped).map(([agent, total]) => ({ agent, total }))
})

async function load() {
  if (!conversationId.value) return
  const res = await omniStatsApi.tokens(conversationId.value)
  stats.value = res.data
}

onMounted(async () => {
  const res = await conversationApi.list()
  conversations.value = res.data || []
  if (conversations.value.length) {
    conversationId.value = conversations.value[0].id
    await load()
  }
})
</script>
