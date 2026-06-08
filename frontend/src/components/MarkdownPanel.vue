<template>
  <div class="markdown-panel">
    <div class="markdown-panel__toolbar" v-if="title || $slots.action">
      <strong>{{ title }}</strong>
      <slot name="action" />
    </div>
    <pre>{{ content || emptyText }}</pre>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  content: String,
  emptyText: { type: String, default: '暂无内容' }
})
</script>
