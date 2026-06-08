<template>
  <ActionPanel
    eyebrow="Requirement Composer"
    title="需求草案生成器"
    description="输入项目标题，生成可编辑的需求文本、建议大纲、模块清单和风险提示。"
  >
    <div class="composer-grid">
      <div class="composer-form">
        <el-form label-position="top">
          <el-form-item label="项目标题">
            <el-input v-model="form.projectTitle" placeholder="例如：在线考试系统" />
          </el-form-item>
          <el-form-item label="补充描述">
            <el-input v-model="form.projectDescription" type="textarea" :rows="4" placeholder="可选：目标用户、业务场景、希望突出的技术点" />
          </el-form-item>
          <el-form-item label="生成模型">
            <ModelSelector v-model="form.modelName" />
          </el-form-item>
          <el-form-item label="技术栈">
            <el-input v-model="form.techStack" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <div class="composer-actions">
          <el-button type="primary" :icon="Sparkles" :loading="loading" @click="generateDraft">生成需求草案</el-button>
          <el-button :icon="ListChecks" :loading="loading" @click="generateDraft">生成建议大纲</el-button>
        </div>
      </div>
      <div class="composer-preview">
        <div class="preview-block">
          <div class="preview-head">
            <strong>建议大纲</strong>
            <el-button size="small" text :disabled="!draft" @click="applyDraft">填入需求</el-button>
          </div>
          <ol v-if="draft?.outline?.length">
            <li v-for="item in draft.outline" :key="item">{{ item }}</li>
          </ol>
          <EmptyState v-else :icon="ListChecks" title="等待生成" description="生成后会显示建议大纲和模块清单。" />
        </div>
        <div class="chip-list" v-if="draft?.suggestedModules?.length">
          <span v-for="item in draft.suggestedModules" :key="item">{{ item }}</span>
        </div>
      </div>
    </div>
  </ActionPanel>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ListChecks, Sparkles } from 'lucide-vue-next'
import { assistantApi } from '../api/http'
import ActionPanel from './ActionPanel.vue'
import EmptyState from './EmptyState.vue'
import ModelSelector from './ModelSelector.vue'

const props = defineProps({
  title: String,
  description: String,
  techStack: String
})

const emit = defineEmits(['draft-generated', 'apply-draft'])

const loading = ref(false)
const draft = ref(null)
const form = reactive({
  projectTitle: props.title || '',
  projectDescription: props.description || '',
  techStack: props.techStack || 'Vue 3 + Element Plus, Spring Boot, MySQL, Redis, FastAPI, LangGraph, Langfuse',
  modelName: 'deepseek-v4-flash'
})

watch(() => props.title, (value) => {
  if (value !== undefined) form.projectTitle = value || ''
})

watch(() => props.description, (value) => {
  if (value !== undefined) form.projectDescription = value || ''
})

watch(() => props.techStack, (value) => {
  if (value) form.techStack = value
})

async function generateDraft() {
  if (!form.projectTitle.trim()) {
    ElMessage.warning('请先输入项目标题')
    return
  }
  loading.value = true
  try {
    const res = await assistantApi.generateRequirementDraft(form)
    draft.value = {
      ...res.data,
      projectTitle: form.projectTitle,
      projectDescription: form.projectDescription,
      techStack: form.techStack,
      modelName: form.modelName
    }
    emit('draft-generated', draft.value)
    ElMessage.success('需求草案已生成')
  } catch (error) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    loading.value = false
  }
}

function applyDraft() {
  if (!draft.value) return
  emit('apply-draft', draft.value)
}
</script>
