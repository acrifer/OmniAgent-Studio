<template>
  <div class="agent-timeline">
    <div v-for="agent in agents" :key="agent.key" class="agent-step" :class="statusClass(agent.key)">
      <div class="agent-step__dot">
        <component :is="agent.icon" :size="16" />
      </div>
      <div>
        <strong>{{ agent.label }}</strong>
        <span>{{ statusText(agent.key) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Bug, ClipboardList, Code2, Layers3, Monitor, ShieldCheck, UserRoundCheck } from 'lucide-vue-next'

const props = defineProps({
  results: { type: Array, default: () => [] },
  taskStatus: { type: Object, default: () => ({}) }
})

const agents = [
  { key: 'PM', label: '产品经理', icon: ClipboardList },
  { key: 'ARCHITECT', label: '架构师', icon: Layers3 },
  { key: 'FRONTEND', label: '前端', icon: Monitor },
  { key: 'BACKEND', label: '后端', icon: Code2 },
  { key: 'TEST', label: '测试', icon: Bug },
  { key: 'SECURITY', label: '安全', icon: ShieldCheck },
  { key: 'SUMMARY', label: '总结', icon: UserRoundCheck }
]

function hasResult(key) {
  return props.results.some((item) => item.agentType === key)
}

function statusClass(key) {
  if (hasResult(key)) return 'is-done'
  if (props.taskStatus?.status === 'RUNNING') return 'is-waiting'
  if (props.taskStatus?.status === 'FAILED') return 'is-failed'
  return 'is-idle'
}

function statusText(key) {
  if (hasResult(key)) return '已生成'
  if (props.taskStatus?.status === 'RUNNING') return '等待执行'
  if (props.taskStatus?.status === 'FAILED') return '执行失败'
  return '未开始'
}
</script>
