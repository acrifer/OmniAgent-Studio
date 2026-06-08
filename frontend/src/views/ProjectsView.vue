<template>
  <section class="section-stack">
    <RequirementComposer
      :title="form.name"
      :description="form.description"
      :tech-stack="defaultTechStack"
      @draft-generated="onDraftGenerated"
      @apply-draft="onDraftGenerated"
    />

    <div class="grid two">
      <ActionPanel eyebrow="Project Intake" title="创建并进入工作区" description="先生成需求草案，再创建项目并保存为首个需求版本。">
        <el-form label-position="top">
          <el-form-item label="项目名称">
            <el-input v-model="form.name" placeholder="例如：在线考试系统" />
          </el-form-item>
          <el-form-item label="项目简介">
            <el-input v-model="form.description" type="textarea" :rows="4" placeholder="补充目标用户、业务场景或技术亮点" />
          </el-form-item>
        </el-form>
        <div class="composer-actions">
          <el-button type="primary" :icon="Plus" :loading="creating" @click="createProject">创建项目并进入工作区</el-button>
          <el-button :icon="RefreshCw" @click="load">刷新列表</el-button>
        </div>
      </ActionPanel>

      <ActionPanel eyebrow="Draft Preview" title="生成结果预览">
        <MarkdownPanel :content="draft?.requirementText" empty-text="生成需求草案后，这里会显示可编辑的项目需求文本。" />
        <div class="chip-list" v-if="draft?.techRisks?.length">
          <span v-for="risk in draft.techRisks" :key="risk">{{ risk }}</span>
        </div>
      </ActionPanel>
    </div>

    <ActionPanel eyebrow="Project Library" title="项目库">
      <template #action>
        <el-segmented v-model="viewMode" :options="['卡片', '表格']" />
      </template>

        <EmptyState v-if="!projects.length" :icon="FolderKanban" title="还没有项目" description="完成一次项目创建后，就可以进入工作区上传文档并启动 Agent 分析。" />

      <div v-else-if="viewMode === '卡片'" class="project-card-grid">
        <article v-for="project in projects" :key="project.id" class="project-card">
          <div class="project-card__meta">
            <span class="status-badge">{{ project.status }}</span>
            <span class="muted">#{{ project.id }}</span>
          </div>
          <h3>{{ project.name }}</h3>
          <p class="muted">{{ project.description || '暂无简介' }}</p>
          <div class="project-card__actions">
            <RouterLink :to="`/projects/${project.id}`"><el-button type="primary" size="small">进入工作区</el-button></RouterLink>
            <el-button size="small" @click="archive(project.id)">归档</el-button>
          </div>
        </article>
      </div>

      <el-table v-else :data="projects">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="description" label="简介" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <RouterLink :to="`/projects/${row.id}`"><el-button size="small" type="primary">进入</el-button></RouterLink>
            <el-button size="small" @click="archive(row.id)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderKanban, Plus, RefreshCw } from 'lucide-vue-next'
import { projectApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import RequirementComposer from '../components/RequirementComposer.vue'

const defaultTechStack = 'Vue 3 + Element Plus, Spring Boot, MySQL, Redis, FastAPI, LangGraph, Langfuse'
const router = useRouter()
const projects = ref([])
const draft = ref(null)
const viewMode = ref('卡片')
const creating = ref(false)
const form = reactive({
  name: '',
  description: ''
})

function onDraftGenerated(value) {
  draft.value = value
  if (value?.projectTitle) {
    form.name = value.projectTitle
  }
  if (!form.description && value?.outline?.length) {
    form.description = value.outline.slice(0, 3).join('、')
  }
}

async function load() {
  const res = await projectApi.list()
  projects.value = res.data
}

async function createProject() {
  if (!form.name.trim()) {
    ElMessage.warning('请先输入项目名称')
    return
  }
  if (!draft.value?.requirementText) {
    ElMessage.warning('请先生成需求草案，再创建项目')
    return
  }
  creating.value = true
  try {
    const res = await projectApi.create(form)
    await projectApi.saveRequirement(res.data.id, { requirementText: draft.value.requirementText })
    ElMessage.success('项目和需求已保存')
    await router.push(`/projects/${res.data.id}`)
  } finally {
    creating.value = false
  }
}

async function archive(id) {
  await projectApi.archive(id)
  ElMessage.success('项目已归档')
  await load()
}

onMounted(load)
</script>
