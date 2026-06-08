<template>
  <section class="action-panel" :class="panelClass">
    <div class="action-panel__head">
      <div class="action-panel__title">
        <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p v-if="description" class="action-panel__description">{{ description }}</p>
      </div>
      <slot name="action" />
    </div>
    <div class="action-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: String,
  title: String,
  description: String,
  density: { type: String, default: 'default' },
  tone: { type: String, default: 'default' }
})

const panelClass = computed(() => ({
  'is-compact': props.density === 'compact',
  'is-dark': props.tone === 'dark'
}))
</script>
