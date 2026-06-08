<template>
  <section class="section-stack">
    <ActionPanel eyebrow="Project Workspace" :title="project?.name || '项目工作区'" :description="project?.description || '加载项目详情中'">
      <template #action>
        <el-button :icon="RefreshCw" :loading="loadingDetail" @click="loadDetail">刷新项目</el-button>
      </template>
    </ActionPanel>

    <RequirementComposer
      :title="project?.name"
      :description="project?.description"
      :tech-stack="taskForm.techStack"
      @draft-generated="generatedDraft = $event"
      @apply-draft="applyDraft"
    />

    <div class="grid two">
      <ActionPanel eyebrow="Step 01" title="需求草案与编辑" description="保存后会生成新的需求版本，启动 Agent 时自动读取最新版本。">
        <template #action>
          <el-button type="primary" :icon="Save" :loading="savingRequirement" @click="saveRequirement">保存需求</el-button>
        </template>
        <el-input v-model="requirementText" type="textarea" :rows="15" placeholder="输入项目背景、核心功能、目标用户和技术约束" />
        <div class="chip-list" v-if="generatedDraft?.suggestedModules?.length">
          <span v-for="module in generatedDraft.suggestedModules" :key="module">{{ module }}</span>
        </div>
      </ActionPanel>

      <ActionPanel eyebrow="Requirement Docs" title="需求文档" description="支持 txt、md、pdf、docx，解析后的文本会并入 Agent 上下文。">
        <template #action>
          <el-upload :show-file-list="false" :http-request="uploadDocument" accept=".txt,.md,.pdf,.docx">
            <el-button type="primary" :icon="UploadCloud" :loading="uploadingDocument">上传文档</el-button>
          </el-upload>
        </template>
        <EmptyState v-if="!documents.length" :icon="FileText" title="暂无文档" description="上传需求说明、原型说明或调研文档后，系统会提取文本摘要。" />
        <div v-else class="inline-list">
          <div v-for="doc in documents" :key="doc.id" class="surface-strip">
            <div class="title-row">
              <strong>{{ doc.fileName }}</strong>
              <span class="status-badge" :class="statusClass(doc.parseStatus)">{{ statusLabel(doc.parseStatus) }}</span>
            </div>
            <p v-if="doc.parsedTextPreview" class="muted doc-preview">{{ doc.parsedTextPreview }}</p>
            <el-alert v-if="doc.errorMessage" type="error" :title="doc.errorMessage" :closable="false" style="margin-top: 10px" />
          </div>
        </div>
      </ActionPanel>
    </div>

    <div class="grid two">
      <ActionPanel eyebrow="Step 02" title="Agent 工作流" description="真实调用 DeepSeek，由 LangGraph 编排 PM、架构、前后端、测试、安全与总结 Agent。">
        <template #action>
          <el-button type="primary" :icon="Rocket" :loading="running" @click="startTask">启动分析</el-button>
        </template>
        <el-form label-position="top">
          <el-form-item label="模型">
            <ModelSelector v-model="taskForm.modelName" />
          </el-form-item>
          <el-form-item label="技术栈">
            <el-input v-model="taskForm.techStack" type="textarea" :rows="5" />
          </el-form-item>
        </el-form>
        <div class="surface-strip">
          <el-progress :percentage="taskStatus.progress || 0" :stroke-width="10" />
          <el-descriptions :column="1" size="small" style="margin-top: 14px">
            <el-descriptions-item label="任务状态">{{ taskStatus.status || '未开始' }}</el-descriptions-item>
            <el-descriptions-item label="任务 ID">{{ taskStatus.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Trace">{{ taskStatus.langfuseTraceId || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="taskStatus.status === 'FAILED'"
            style="margin-top: 12px"
            type="error"
            :title="taskStatus.errorMessage || 'AI 服务执行失败，请检查 DeepSeek API Key、模型名称或 FastAPI 服务状态。'"
            :closable="false"
          />
        </div>
      </ActionPanel>

      <ActionPanel eyebrow="Step 03" title="Agent 执行过程">
        <template #action>
          <el-button :icon="RefreshCw" @click="refreshTask" :disabled="!taskId">刷新</el-button>
        </template>
        <AgentTimeline :results="results" :task-status="taskStatus" />
      </ActionPanel>
    </div>

    <div class="grid two">
      <ActionPanel eyebrow="Final Report" title="最终研发报告">
        <template #action>
          <el-button @click="loadReport">查看最新报告</el-button>
        </template>
        <MarkdownPanel :content="report?.contentMarkdown" empty-text="Agent 工作流完成后会在这里展示最终研发方案报告。" />
      </ActionPanel>

      <ActionPanel eyebrow="Agent Outputs" title="中间结果与局部重生成">
        <template #action>
          <el-button @click="loadResults" :disabled="!taskId">加载结果</el-button>
        </template>
        <EmptyState v-if="!results.length" :icon="Boxes" title="暂无 Agent 输出" description="启动工作流后，各 Agent 的结构化结果会陆续出现在这里。" />
        <el-tabs v-else v-model="activeTab">
          <el-tab-pane v-for="item in results" :key="item.id" :label="item.agentType" :name="String(item.id)">
            <MarkdownPanel :content="item.contentMarkdown">
              <template #action>
                <el-button size="small" :loading="regenerating === item.agentType" @click="regenerate(item.agentType)">局部重生成</el-button>
              </template>
            </MarkdownPanel>
          </el-tab-pane>
        </el-tabs>
      </ActionPanel>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Boxes, FileText, RefreshCw, Rocket, Save, UploadCloud } from 'lucide-vue-next'
import { projectApi, taskApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import AgentTimeline from '../components/AgentTimeline.vue'
import EmptyState from '../components/EmptyState.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import ModelSelector from '../components/ModelSelector.vue'
import RequirementComposer from '../components/RequirementComposer.vue'

const route = useRoute()
const projectId = route.params.id
const project = ref(null)
const documents = ref([])
const requirementText = ref('')
const generatedDraft = ref(null)
const taskForm = reactive({
  modelName: 'deepseek-v4-flash',
  techStack: 'Vue 3 + Element Plus, Spring Boot, MySQL, Redis, FastAPI, LangGraph, Langfuse'
})
const taskId = ref(null)
const taskStatus = ref({})
const results = ref([])
const report = ref(null)
const activeTab = ref('')
const running = ref(false)
const loadingDetail = ref(false)
const savingRequirement = ref(false)
const uploadingDocument = ref(false)
const regenerating = ref('')
let timer = null

function applyDraft(draft) {
  generatedDraft.value = draft
  requirementText.value = draft.requirementText
}

async function loadDetail() {
  loadingDetail.value = true
  try {
    const res = await projectApi.detail(projectId)
    const detail = res.data
    project.value = detail.project
    documents.value = detail.documents || []
    report.value = detail.latestReport
    if (detail.latestRequirement?.requirementText) {
      requirementText.value = detail.latestRequirement.requirementText
    }
    if (detail.latestTask) {
      taskStatus.value = detail.latestTask
      taskId.value = detail.latestTask.id
      await loadResults()
      if (detail.latestTask.status === 'RUNNING') startPolling()
    }
  } finally {
    loadingDetail.value = false
  }
}

async function saveRequirement() {
  if (!requirementText.value.trim()) {
    ElMessage.warning('请先填写需求内容')
    return false
  }
  savingRequirement.value = true
  try {
    await projectApi.saveRequirement(projectId, { requirementText: requirementText.value })
    ElMessage.success('需求已保存')
    return true
  } finally {
    savingRequirement.value = false
  }
}

async function uploadDocument(option) {
  uploadingDocument.value = true
  try {
    const res = await projectApi.uploadDocument(projectId, option.file)
    documents.value.unshift(res.data)
    if (res.data.parseStatus === 'PARSED') {
      ElMessage.success('文档已解析')
    } else {
      ElMessage.error(res.data.errorMessage || '文档解析失败')
    }
    option.onSuccess?.(res.data)
  } catch (error) {
    option.onError?.(error)
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploadingDocument.value = false
  }
}

async function startTask() {
  running.value = true
  try {
    const saved = await saveRequirement()
    if (!saved) return
    const res = await projectApi.startTask(projectId, taskForm)
    taskId.value = res.data.id
    taskStatus.value = res.data
    results.value = []
    activeTab.value = ''
    ElMessage.success(res.data.status === 'FAILED' ? '任务启动失败，请查看错误提示' : '任务已启动')
    startPolling()
  } finally {
    running.value = false
  }
}

async function refreshTask() {
  if (!taskId.value) return
  const res = await taskApi.status(taskId.value)
  taskStatus.value = res.data
  await loadResults()
  if (['SUCCESS', 'FAILED'].includes(res.data.status)) {
    clearInterval(timer)
    await loadReport()
  }
}

async function loadResults() {
  if (!taskId.value) return
  const res = await taskApi.results(taskId.value)
  results.value = res.data
  if (results.value.length && !activeTab.value) activeTab.value = String(results.value[0].id)
}

async function loadReport() {
  const res = await projectApi.latestReport(projectId)
  report.value = res.data
}

async function regenerate(agentType) {
  if (!taskId.value) return
  regenerating.value = agentType
  try {
    const res = await taskApi.regenerate(taskId.value, { agentType })
    taskId.value = res.data.id
    taskStatus.value = res.data
    results.value = []
    activeTab.value = ''
    ElMessage.success(`${agentType} 已开始重生成`)
    startPolling()
  } finally {
    regenerating.value = ''
  }
}

function startPolling() {
  clearInterval(timer)
  refreshTask()
  timer = setInterval(refreshTask, 1800)
}

function statusLabel(status) {
  const labels = { PARSED: '已解析', FAILED: '解析失败', STORED: '已保存' }
  return labels[status] || status || '未知'
}

function statusClass(status) {
  return {
    PARSED: 'is-success',
    FAILED: 'is-danger',
    STORED: 'is-muted'
  }[status] || 'is-muted'
}

onMounted(loadDetail)
onBeforeUnmount(() => clearInterval(timer))
</script>
