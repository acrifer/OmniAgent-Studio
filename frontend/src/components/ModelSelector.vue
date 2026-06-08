<template>
  <el-select :model-value="modelValue" class="model-selector" @update:model-value="$emit('update:modelValue', $event)">
    <el-option label="DeepSeek V4 Flash" value="deepseek-v4-flash" />
    <el-option label="DeepSeek V4 Pro" value="deepseek-v4-pro" />
    <el-option label="DeepSeek Chat 兼容" value="deepseek-chat" />
    <el-option label="DeepSeek Reasoner 兼容" value="deepseek-reasoner" />
  </el-select>
</template>

<script setup>
defineProps({
  modelValue: String
})

defineEmits(['update:modelValue'])
</script>
