<template>
  <section class="action-panel">
    <div class="action-panel__head">
      <div>
        <p class="eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
      </div>
      <slot name="action" />
    </div>
    <p v-if="description" class="muted">{{ description }}</p>
    <slot />
  </section>
</template>

<script setup>
defineProps({
  eyebrow: String,
  title: String,
  description: String
})
</script>
