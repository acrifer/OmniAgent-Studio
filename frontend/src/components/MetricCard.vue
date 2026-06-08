<template>
  <section class="metric-card">
    <div class="metric-icon">
      <component :is="icon" :size="20" />
    </div>
    <div class="metric-content">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
      <small>{{ hint }}</small>
    </div>
  </section>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  hint: String,
  icon: Object
})
</script>
