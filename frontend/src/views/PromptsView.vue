<template>
  <section class="grid two">
    <ActionPanel eyebrow="Prompt Studio" title="新增 Prompt" description="管理 Agent 角色、约束和输出格式，支持版本对比与质量复盘。">
      <template #action>
        <el-button type="primary" :icon="Save" @click="createPrompt">保存模板</el-button>
      </template>
      <el-form label-position="top">
        <el-form-item label="Agent 类型"><el-input v-model="form.agentType" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="版本"><el-input v-model="form.version" /></el-form-item>
        <el-form-item label="模型"><ModelSelector v-model="form.modelName" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="12" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Template Library" title="模板列表">
      <EmptyState v-if="!prompts.length" :icon="FileCode2" title="暂无模板" description="保存 Prompt 后会在这里展示。" />
      <el-table v-else :data="prompts">
        <el-table-column prop="agentType" label="Agent" width="120" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="modelName" label="模型" width="160" />
      </el-table>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { FileCode2, Save } from 'lucide-vue-next'
import { promptApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import ModelSelector from '../components/ModelSelector.vue'

const prompts = ref([])
const form = reactive({
  agentType: 'PM',
  name: '产品经理 Agent Prompt',
  version: 'v1',
  modelName: 'deepseek-v4-flash',
  temperature: 0.2,
  content: '你是资深软件产品经理，请输出结构化需求分析。'
})

async function load() {
  const res = await promptApi.list()
  prompts.value = res.data
}

async function createPrompt() {
  await promptApi.create(form)
  ElMessage.success('Prompt 已保存')
  await load()
}

onMounted(load)
</script>
