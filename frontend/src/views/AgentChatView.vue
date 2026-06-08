<template>
  <section class="agent-workbench">
    <ActionPanel class="conversation-rail" density="compact" eyebrow="Sessions" title="会话">
      <template #action>
        <el-button type="primary" :icon="Plus" circle @click="createConversation" />
      </template>
      <p class="muted" style="margin: 0;">问题和运行记录按会话归档</p>
      <div class="inline-list conversation-list">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="conversation-item"
          :class="{ active: item.id === activeConversationId }"
          @click="selectConversation(item.id)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.mode }} · {{ item.modelName }}</span>
        </button>
      </div>
      <EmptyState v-if="!conversations.length" :icon="Bot" title="暂无会话" description="创建一个会话后即可开始提问。" compact />
    </ActionPanel>

    <div class="section-stack">
      <ActionPanel eyebrow="Ask OmniAgent" title="一次完整的智能体任务" description="选择能力模式，附加文件或知识库，然后启动多 Agent 协作。">
        <template #action>
          <div class="actions">
            <el-upload :auto-upload="false" :show-file-list="true" :on-change="uploadConversationFile">
              <el-button :icon="UploadCloud">上传文档或图片</el-button>
            </el-upload>
            <el-button type="primary" :icon="Send" :loading="running" @click="submitQuestion">启动智能体</el-button>
          </div>
        </template>
        <div class="composer-grid">
          <div class="composer-form">
            <el-form label-position="top">
              <el-form-item label="问题">
                <el-input v-model="question" type="textarea" :rows="8" placeholder="例如：总结我上传的论文，并结合知识库给出可执行方案" />
              </el-form-item>
              <div class="mode-grid">
                <button v-for="mode in modes" :key="mode.value" class="mode-card" :class="{ active: form.mode === mode.value }" @click.prevent="form.mode = mode.value">
                  <component :is="mode.icon" :size="18" />
                  <strong>{{ mode.label }}</strong>
                  <span>{{ mode.hint }}</span>
                </button>
              </div>
              <div class="grid two" style="margin-top: 16px;">
                <el-form-item label="模型">
                  <el-select v-model="form.modelName" style="width: 100%">
                    <el-option label="DeepSeek V4 Flash" value="deepseek-v4-flash" />
                    <el-option label="Qwen Plus" value="qwen-plus" />
                    <el-option label="GPT-4o Mini" value="gpt-4o-mini" />
                  </el-select>
                </el-form-item>
                <el-form-item label="知识库">
                  <el-select v-model="form.knowledgeBaseId" clearable placeholder="可选" style="width: 100%">
                    <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
                  </el-select>
                </el-form-item>
              </div>
            </el-form>
          </div>
          <div class="composer-preview">
            <div class="preview-block">
              <div class="preview-head">
                <strong>上下文文件</strong>
                <span class="status-badge">{{ detail?.files?.length || 0 }} 个文件</span>
              </div>
              <EmptyState v-if="!(detail?.files || []).length" :icon="UploadCloud" title="暂无上下文" description="上传文档或图片后会在这里显示解析状态。" compact />
              <div v-else class="inline-list">
                <div v-for="file in detail?.files || []" :key="file.id" class="surface-strip">
                  <div class="title-row">
                    <strong>{{ file.fileName }}</strong>
                    <span class="status-badge" :class="statusClass(file.parseStatus)">{{ file.parseStatus }}</span>
                  </div>
                  <p class="muted doc-preview">{{ file.errorMessage || file.parsedTextPreview || '图片文件将交给 Vision Agent 处理' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </ActionPanel>

      <div class="console-grid">
        <ActionPanel class="span-7" eyebrow="Agent Graph" title="执行可视化" tone="dark">
          <div class="agent-flow">
            <VueFlow :nodes="flowNodes" :edges="flowEdges" fit-view-on-init :nodes-draggable="false" :zoom-on-scroll="false">
              <Background pattern-color="#2f6fed" :gap="18" />
            </VueFlow>
          </div>
        </ActionPanel>
        <ActionPanel class="span-5" eyebrow="Trace Timeline" title="步骤详情">
          <EmptyState v-if="!steps.length" :icon="Workflow" title="等待运行" description="启动智能体后，这里会显示每个 Agent 的输入、输出、Token 和耗时。" compact />
          <div v-else class="inline-list">
            <button v-for="step in steps" :key="step.id" class="step-card" @click="selectedStep = step">
              <div class="title-row">
                <strong>{{ step.agentType }}</strong>
                <span class="status-badge" :class="statusClass(step.status)">{{ step.status }}</span>
              </div>
              <p class="muted">{{ step.modelName || 'system' }} · {{ step.promptTokens || 0 }} / {{ step.completionTokens || 0 }} tokens · {{ step.latencyMs || 0 }}ms</p>
              <p v-if="step.errorMessage" class="error-text">{{ step.errorMessage }}</p>
            </button>
          </div>
        </ActionPanel>
      </div>

      <MarkdownPanel title="最终回答" :content="answer.answerMarkdown" empty-text="智能体完成后会在这里展示带引用的最终回答。" />
    </div>
  </section>

  <el-drawer v-model="drawerVisible" title="Agent Step Detail" size="48%">
    <MarkdownPanel :title="selectedStep?.agentType" :content="selectedStep?.contentMarkdown || selectedStep?.errorMessage" />
    <div class="grid two" style="margin-top: 14px;">
      <MarkdownPanel title="输入" :content="selectedStep?.inputJson" code />
      <MarkdownPanel title="输出 JSON" :content="selectedStep?.outputJson" code />
    </div>
  </el-drawer>
</template>

<script setup>
import '@vue-flow/core/dist/style.css'
import { Background } from '@vue-flow/background'
import { VueFlow } from '@vue-flow/core'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Bot, BrainCircuit, FileSearch, Globe2, Image, LibraryBig, Plus, Send, UploadCloud, Wrench, Workflow } from 'lucide-vue-next'
import { agentRunApi, conversationApi, knowledgeApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'

const modes = [
  { value: 'AUTO', label: '自动模式', hint: '按任务自动选择 Agent', icon: BrainCircuit },
  { value: 'SEARCH', label: '联网搜索', hint: '需要 Tavily Key', icon: Globe2 },
  { value: 'RAG', label: '知识库问答', hint: '基于入库文档回答', icon: LibraryBig },
  { value: 'DOCUMENT', label: '文档阅读', hint: '总结上传资料', icon: FileSearch },
  { value: 'VISION', label: '图片理解', hint: '需要视觉模型', icon: Image },
  { value: 'TOOL', label: '工具调用', hint: '调用 MCP/Skill', icon: Wrench }
]
const conversations = ref([])
const knowledgeBases = ref([])
const activeConversationId = ref(null)
const detail = ref(null)
const question = ref('')
const running = ref(false)
const activeRun = ref(null)
const steps = ref([])
const answer = ref({})
const selectedStep = ref(null)
const timer = ref(null)
const form = reactive({ mode: 'AUTO', knowledgeBaseId: null, modelName: 'deepseek-v4-flash' })

const drawerVisible = computed({
  get: () => Boolean(selectedStep.value),
  set: (value) => { if (!value) selectedStep.value = null }
})

const nodePositions = {
  PLANNER: { x: 10, y: 150 },
  SEARCH: { x: 150, y: 20 },
  READER: { x: 150, y: 100 },
  RAG: { x: 150, y: 180 },
  VISION: { x: 290, y: 60 },
  TOOL: { x: 290, y: 180 },
  CRITIC: { x: 150, y: 300 },
  ANSWER: { x: 290, y: 300 }
}
const flowNodes = computed(() => Object.keys(nodePositions).map((agent) => ({
  id: agent,
  label: `${agent}\n${nodeStatus(agent)}`,
  position: nodePositions[agent],
  class: `flow-node ${nodeStatus(agent).toLowerCase()}`
})))
const flowEdges = [
  ['PLANNER', 'SEARCH'], ['PLANNER', 'READER'], ['PLANNER', 'RAG'], ['PLANNER', 'VISION'], ['PLANNER', 'TOOL'],
  ['SEARCH', 'CRITIC'], ['READER', 'CRITIC'], ['RAG', 'CRITIC'], ['VISION', 'CRITIC'], ['TOOL', 'CRITIC'],
  ['CRITIC', 'ANSWER']
].map(([source, target]) => ({ id: `${source}-${target}`, source, target, animated: true }))

function nodeStatus(agent) {
  const latest = [...steps.value].reverse().find((step) => step.agentType === agent)
  if (latest) return latest.status
  return activeRun.value?.status === 'RUNNING' ? 'PENDING' : 'WAITING'
}

function statusClass(status) {
  if (['SUCCESS', 'PARSED', 'READY'].includes(status)) return 'is-success'
  if (['FAILED'].includes(status)) return 'is-danger'
  if (['SKIPPED', 'IMAGE', 'WAITING'].includes(status)) return 'is-muted'
  return ''
}

async function loadBase() {
  const [conversationRes, kbRes] = await Promise.all([conversationApi.list(), knowledgeApi.list()])
  conversations.value = conversationRes.data || []
  knowledgeBases.value = kbRes.data || []
  if (!activeConversationId.value && conversations.value.length) await selectConversation(conversations.value[0].id)
}

async function createConversation() {
  const res = await conversationApi.create({ title: '新的智能体会话', mode: form.mode, modelName: form.modelName })
  conversations.value.unshift(res.data)
  await selectConversation(res.data.id)
}

async function selectConversation(id) {
  activeConversationId.value = id
  const res = await conversationApi.detail(id)
  detail.value = res.data
  activeRun.value = res.data.latestRun
  steps.value = []
  answer.value = {}
  if (activeRun.value) await refreshRun()
}

async function uploadConversationFile(uploadFile) {
  if (!activeConversationId.value) await createConversation()
  try {
    await conversationApi.uploadFile(activeConversationId.value, uploadFile.raw)
    await selectConversation(activeConversationId.value)
    ElMessage.success('文件已上传并解析')
  } catch (e) {
    ElMessage.error(e.message || '文件上传失败')
  }
}

async function submitQuestion() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  if (!activeConversationId.value) await createConversation()
  running.value = true
  try {
    const msg = await conversationApi.sendMessage(activeConversationId.value, { content: question.value })
    const run = await agentRunApi.start({
      conversationId: activeConversationId.value,
      messageId: msg.data.id,
      question: question.value,
      mode: form.mode,
      knowledgeBaseId: form.knowledgeBaseId,
      modelName: form.modelName
    })
    activeRun.value = run.data
    steps.value = []
    answer.value = {}
    pollRun()
  } catch (e) {
    running.value = false
    ElMessage.error(e.message || '智能体启动失败')
  }
}

function pollRun() {
  clearInterval(timer.value)
  timer.value = setInterval(refreshRun, 1800)
  refreshRun()
}

async function refreshRun() {
  if (!activeRun.value?.id) return
  const [statusRes, stepsRes, answerRes] = await Promise.all([
    agentRunApi.status(activeRun.value.id),
    agentRunApi.steps(activeRun.value.id),
    agentRunApi.answer(activeRun.value.id)
  ])
  activeRun.value = statusRes.data
  steps.value = stepsRes.data || []
  answer.value = answerRes.data || {}
  if (['SUCCESS', 'FAILED'].includes(activeRun.value.status)) {
    running.value = false
    clearInterval(timer.value)
    if (activeRun.value.status === 'FAILED') ElMessage.error(activeRun.value.errorMessage || '智能体运行失败')
    nextTick(() => loadBase())
  }
}

watch(activeConversationId, () => clearInterval(timer.value))
onMounted(async () => {
  await loadBase()
  if (!conversations.value.length) await createConversation()
})
onBeforeUnmount(() => clearInterval(timer.value))
</script>
