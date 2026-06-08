<template>
  <section class="grid two">
    <ActionPanel eyebrow="Model Providers" title="模型供应商" description="保存模型供应商元数据。真实 API Key 仍建议放在服务端环境变量中。">
      <template #action><el-button type="primary" :icon="Save" @click="save">保存配置</el-button></template>
      <el-form label-position="top">
        <el-form-item label="供应商"><el-select v-model="form.provider" style="width:100%"><el-option label="DeepSeek" value="deepseek" /><el-option label="通义千问" value="qwen" /><el-option label="OpenAI" value="openai" /></el-select></el-form-item>
        <el-form-item label="模型"><el-input v-model="form.modelName" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="form.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Configured Models" title="已保存配置">
      <div class="inline-list">
        <div v-for="item in providers" :key="item.id" class="surface-strip">
          <div class="title-row"><strong>{{ item.provider }}</strong><span class="status-badge" :class="{ 'is-success': item.enabled }">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span></div>
          <p class="muted">{{ item.modelName }} · {{ item.baseUrl }}</p>
        </div>
      </div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Save } from 'lucide-vue-next'
import { modelProviderApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'

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
