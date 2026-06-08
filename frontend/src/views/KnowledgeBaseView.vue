<template>
  <section class="grid two">
    <ActionPanel eyebrow="Vector RAG" title="知识库" description="上传文档后解析文本、切片并写入 Qdrant，RAG Agent 按语义相似度检索可引用片段。">
      <template #action>
        <el-button type="primary" :icon="Plus" @click="createKb">新建知识库</el-button>
      </template>
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="例如：产品资料库" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div class="inline-list">
        <button v-for="kb in knowledgeBases" :key="kb.id" class="conversation-item" :class="{ active: kb.id === activeKbId }" @click="selectKb(kb.id)">
          <strong>{{ kb.name }}</strong>
          <span>{{ kb.vectorStatus || 'EMPTY' }}</span>
        </button>
      </div>
    </ActionPanel>

    <ActionPanel eyebrow="Documents" title="文档入库">
      <template #action>
        <el-upload v-if="activeKbId" :auto-upload="false" :show-file-list="false" :on-change="uploadDoc">
          <el-button :icon="UploadCloud">上传文档</el-button>
        </el-upload>
      </template>
      <EmptyState v-if="!activeKbId" :icon="LibraryBig" title="选择知识库" description="选择或新建知识库后上传文档。" />
      <EmptyState v-else-if="!documents.length" :icon="LibraryBig" title="暂无文档" description="支持 txt、md、pdf、docx，入库成功后可在智能体对话中启用 RAG。" />
      <div v-else class="inline-list">
        <div v-for="doc in documents" :key="doc.id" class="surface-strip">
          <div class="title-row">
            <strong>{{ doc.fileName }}</strong>
            <span class="status-badge" :class="statusClass(doc.ingestStatus)">{{ doc.ingestStatus }}</span>
          </div>
          <p class="muted" style="margin: 4px 0 8px;">向量库：Qdrant · 状态：{{ doc.ingestStatus }}</p>
          <p class="muted doc-preview">{{ doc.errorMessage || doc.parsedTextPreview }}</p>
        </div>
      </div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { LibraryBig, Plus, UploadCloud } from 'lucide-vue-next'
import { knowledgeApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'

const knowledgeBases = ref([])
const documents = ref([])
const activeKbId = ref(null)
const form = reactive({ name: '', description: '' })

function statusClass(status) {
  if (status === 'READY') return 'is-success'
  if (status === 'FAILED') return 'is-danger'
  return ''
}

async function load() {
  const res = await knowledgeApi.list()
  knowledgeBases.value = res.data || []
  if (!activeKbId.value && knowledgeBases.value.length) await selectKb(knowledgeBases.value[0].id)
}

async function createKb() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  const res = await knowledgeApi.create(form)
  form.name = ''
  form.description = ''
  activeKbId.value = res.data.id
  await load()
  await selectKb(res.data.id)
}

async function selectKb(id) {
  activeKbId.value = id
  const res = await knowledgeApi.documents(id)
  documents.value = res.data || []
}

async function uploadDoc(uploadFile) {
  try {
    await knowledgeApi.uploadDocument(activeKbId.value, uploadFile.raw)
    await selectKb(activeKbId.value)
    ElMessage.success('文档已解析并入库')
  } catch (e) {
    ElMessage.error(e.message || '文档入库失败')
  }
}

onMounted(load)
</script>
