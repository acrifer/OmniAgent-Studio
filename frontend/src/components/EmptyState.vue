<template>
  <div class="empty-state" :class="{ 'is-compact': compact }">
    <div class="empty-state__inner">
      <div class="empty-state__icon">
        <component :is="icon" :size="28" />
      </div>
      <strong>{{ title }}</strong>
      <p>{{ description }}</p>
      <slot />
    </div>
  </div>
</template>

<script setup>
import { Inbox } from 'lucide-vue-next'

defineProps({
  icon: { type: Object, default: Inbox },
  title: String,
  description: String,
  compact: Boolean
})
</script>
