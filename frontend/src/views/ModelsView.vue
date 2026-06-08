<template>
  <section class="config-layout">
    <ActionPanel eyebrow="Model Providers" title="模型供应商" description="保存 LLM 供应商元数据。真实 API Key 仍放在服务端环境变量中；Embedding 与 Vision 也复用 OpenAI-compatible 接口。">
      <template #action><el-button type="primary" :icon="Save" @click="save">保存配置</el-button></template>
      <el-form label-position="top">
        <el-form-item label="供应商"><el-select v-model="form.provider" style="width:100%"><el-option label="DeepSeek" value="deepseek" /><el-option label="通义千问" value="qwen" /><el-option label="OpenAI" value="openai" /></el-select></el-form-item>
        <el-form-item label="模型"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="form.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <div class="section-stack">
      <ActionPanel eyebrow="Configured Models" title="已保存配置">
        <EmptyState v-if="!providers.length" title="暂无模型配置" description="保存供应商元数据后会显示在这里。" compact />
        <div v-else class="inline-list">
          <div v-for="item in providers" :key="item.id" class="surface-strip">
            <div class="title-row"><strong>{{ item.provider }}</strong><span class="status-badge" :class="{ 'is-success': item.enabled }">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span></div>
            <p class="muted">{{ item.modelName }} · {{ item.baseUrl }}</p>
          </div>
        </div>
      </ActionPanel>
      <ActionPanel eyebrow="Runtime Roles" title="运行时模型角色">
        <div class="inline-list">
          <div class="surface-strip"><strong>LLM</strong><p class="muted">Planner / Reader / Critic / Answer 使用当前会话模型。</p></div>
          <div class="surface-strip"><strong>Embedding</strong><p class="muted">知识库入库与检索使用服务端 `EMBEDDING_MODEL`。</p></div>
          <div class="surface-strip"><strong>Vision</strong><p class="muted">图片理解使用服务端 `VISION_MODEL`，通过 OpenAI-compatible 多模态接口调用。</p></div>
        </div>
      </ActionPanel>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Save } from 'lucide-vue-next'
import { modelProviderApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'

const providers = ref([])
const form = reactive({ provider: 'deepseek', modelName: 'deepseek-v4-flash', baseUrl: 'https://api.deepseek.com', enabled: true, configJson: '{}' })
async function load() {
  const res = await modelProviderApi.list()
  providers.value = res.data || []
}
async function save() {
  await modelProviderApi.save(form)
  ElMessage.success('模型配置已保存')
  await load()
}
onMounted(load)
</script>
