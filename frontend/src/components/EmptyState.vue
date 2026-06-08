<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <component :is="icon" :size="28" />
    </div>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
import { Inbox } from 'lucide-vue-next'

defineProps({
  icon: { type: Object, default: Inbox },
  title: String,
  description: String
})
</script>
