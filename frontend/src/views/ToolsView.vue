<template>
  <section class="grid two">
    <ActionPanel eyebrow="Tool Agent" title="工具配置" description="配置可被 Tool Agent 调用的 HTTP/MCP endpoint；运行时会把问题和 Agent 上下文作为 JSON 传入工具。">
      <template #action><el-button type="primary" :icon="Save" @click="saveTool">保存工具</el-button></template>
      <el-form label-position="top">
        <el-form-item label="工具名称"><el-input v-model="tool.name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="tool.toolType" style="width:100%"><el-option label="MCP" value="MCP" /><el-option label="HTTP" value="HTTP" /></el-select></el-form-item>
        <el-form-item label="Endpoint"><el-input v-model="tool.endpoint" placeholder="http://localhost:8000/ai/tools/demo/echo" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="tool.enabled" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="tool.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Skill Registry" title="技能注册" description="注册本地 Skill 名称和能力描述，Planner 和 Tool Agent 会把它作为可选能力上下文。">
      <template #action><el-button type="primary" :icon="Save" @click="saveSkill">保存技能</el-button></template>
      <el-form label-position="top">
        <el-form-item label="技能名称"><el-input v-model="skill.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="skill.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="skill.enabled" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="skill.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Tool Inventory" title="已配置工具">
      <div class="inline-list"><div v-for="item in tools" :key="item.id" class="surface-strip"><div class="title-row"><strong>{{ item.name }}</strong><span class="status-badge" :class="{ 'is-success': item.enabled }">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span></div><p class="muted">{{ item.toolType }} · {{ item.endpoint }}</p></div></div>
    </ActionPanel>
    <ActionPanel eyebrow="Skill Inventory" title="已注册技能">
      <div class="inline-list"><div v-for="item in skills" :key="item.id" class="surface-strip"><div class="title-row"><strong>{{ item.name }}</strong><span class="status-badge" :class="{ 'is-success': item.enabled }">{{ item.enabled ? 'ENABLED' : 'DISABLED' }}</span></div><p class="muted">{{ item.description }}</p></div></div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Save } from 'lucide-vue-next'
import { skillApi, toolApi } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'

const tools = ref([])
const skills = ref([])
const tool = reactive({ toolType: 'MCP', name: '', endpoint: '', enabled: true, configJson: '{}' })
const skill = reactive({ name: '', description: '', enabled: true, configJson: '{}' })

async function load() {
  const [toolRes, skillRes] = await Promise.all([toolApi.list(), skillApi.list()])
  tools.value = toolRes.data || []
  skills.value = skillRes.data || []
}
async function saveTool() {
  await toolApi.save(tool)
  ElMessage.success('工具已保存')
  await load()
}
async function saveSkill() {
  await skillApi.save(skill)
  ElMessage.success('技能已保存')
  await load()
}
onMounted(load)
</script>
