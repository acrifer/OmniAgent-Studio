<template>
  <section class="grid two">
    <ActionPanel eyebrow="MCP Tools" title="工具配置" description="配置可被 Tool Agent 调用的 MCP 或 HTTP 工具 endpoint。">
      <template #action><el-button type="primary" :icon="Save" @click="saveTool">保存工具</el-button></template>
      <el-form label-position="top">
        <el-form-item label="工具名称"><el-input v-model="tool.name" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="tool.toolType" style="width:100%"><el-option label="MCP" value="MCP" /><el-option label="HTTP" value="HTTP" /></el-select></el-form-item>
        <el-form-item label="Endpoint"><el-input v-model="tool.endpoint" placeholder="https://example.com/mcp-call" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="tool.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Local Skills" title="技能注册" description="注册本地 Skill 名称和能力描述，供后续 Tool Agent 选择。">
      <template #action><el-button type="primary" :icon="Save" @click="saveSkill">保存技能</el-button></template>
      <el-form label-position="top">
        <el-form-item label="技能名称"><el-input v-model="skill.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="skill.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="skill.configJson" type="textarea" :rows="5" /></el-form-item>
      </el-form>
    </ActionPanel>
    <ActionPanel eyebrow="Tool Inventory" title="已配置工具">
      <div class="inline-list"><div v-for="item in tools" :key="item.id" class="surface-strip"><strong>{{ item.name }}</strong><p class="muted">{{ item.toolType }} · {{ item.endpoint }}</p></div></div>
    </ActionPanel>
    <ActionPanel eyebrow="Skill Inventory" title="已注册技能">
      <div class="inline-list"><div v-for="item in skills" :key="item.id" class="surface-strip"><strong>{{ item.name }}</strong><p class="muted">{{ item.description }}</p></div></div>
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
