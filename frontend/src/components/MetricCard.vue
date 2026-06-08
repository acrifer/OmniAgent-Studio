<template>
  <section class="metric-card" :class="toneClass">
    <div class="metric-icon">
      <component :is="icon" :size="20" />
    </div>
    <div class="metric-content">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
      <div v-if="trend" class="metric-trend">{{ trend }}</div>
      <small>{{ hint }}</small>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  value: [String, Number],
  hint: String,
  icon: Object,
  trend: String,
  tone: { type: String, default: 'default' }
})

const toneClass = computed(() => {
  if (!props.tone || props.tone === 'default') return null
  return `is-${props.tone}`
})
</script>
