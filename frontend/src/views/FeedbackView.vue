<template>
  <section class="grid two">
    <ActionPanel eyebrow="Human Feedback" title="提交反馈" description="把人工评分写入业务库；配置 Langfuse 后可同步为 Score。">
      <template #action>
        <el-button type="primary" :icon="Send" @click="submit">提交反馈</el-button>
      </template>
      <el-form label-position="top">
        <el-form-item label="会话">
          <el-select v-model="conversationId" placeholder="选择会话" style="width: 100%">
            <el-option v-for="item in conversations" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运行 ID"><el-input v-model="form.taskId" /></el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option value="ANSWER" label="最终回答" />
            <el-option value="AGENT_STEP" label="Agent 步骤" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 ID"><el-input v-model="form.targetId" /></el-form-item>
        <el-form-item label="评分"><el-rate v-model="form.rating" :max="5" /></el-form-item>
        <el-form-item label="意见"><el-input v-model="form.comment" type="textarea" :rows="6" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Feedback Loop" title="反馈如何产生价值">
      <div class="inline-list">
        <div class="surface-strip"><strong>结果质量</strong><p class="muted">定位哪些 Agent 输出需要优化。</p></div>
        <div class="surface-strip"><strong>Prompt 与 RAG 迭代</strong><p class="muted">对低分输出回看 Prompt、引用和 Trace。</p></div>
        <div class="surface-strip"><strong>产品闭环</strong><p class="muted">评分能帮助识别回答不完整、引用不足或工具失败。</p></div>
      </div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Send } from 'lucide-vue-next'
import { conversationApi, feedbackApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'

const conversations = ref([])
const conversationId = ref(null)
const form = reactive({
  projectId: null,
  taskId: null,
  targetType: 'ANSWER',
  targetId: null,
  rating: 4,
  comment: ''
})

async function submit() {
  if (!conversationId.value) {
    ElMessage.warning('请选择会话')
    return
  }
  form.projectId = conversationId.value
  await feedbackApi.create(form)
  ElMessage.success('反馈已提交')
}

onMounted(async () => {
  const res = await conversationApi.list()
  conversations.value = res.data || []
  if (conversations.value.length) conversationId.value = conversations.value[0].id
})
</script>
