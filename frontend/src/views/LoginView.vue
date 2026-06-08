<template>
  <div class="login-page">
    <section class="login-showcase">
      <p class="page-kicker">OmniAgent Studio</p>
      <h1>面向真实任务的企业级智能体控制台</h1>
      <p class="subtitle">将 Planner、RAG、Vision、Tool、Critic 与 Answer 汇聚在一个稳定的业务控制面，既能执行任务，也能回看每一步证据、Token 和延迟。</p>
      <div class="command-strip">
        <span class="command-pill">Planner</span>
        <span class="command-pill">RAG</span>
        <span class="command-pill">Vision</span>
        <span class="command-pill">Tool</span>
        <span class="command-pill">Trace</span>
      </div>
      <div class="login-graph-card">
        <div class="login-node" style="left: 58px; top: 138px;">PLANNER</div>
        <div class="login-node" style="left: 248px; top: 60px;">SEARCH</div>
        <div class="login-node" style="left: 248px; top: 138px;">RAG</div>
        <div class="login-node" style="left: 248px; top: 216px;">TOOL</div>
        <div class="login-node" style="left: 456px; top: 100px;">CRITIC</div>
        <div class="login-node" style="left: 456px; top: 192px;">ANSWER</div>
      </div>
    </section>
    <section class="login-card-wrap">
      <div class="login-card">
        <p class="page-kicker">Secure Workspace</p>
        <h2 style="margin-top: 8px;">登录控制台</h2>
        <p class="subtitle">使用测试账号进入控制台，查看会话、知识库、模型和多 Agent 执行数据。</p>
        <div class="surface-strip" style="margin: 18px 0;">
          <strong>测试账号</strong>
          <p class="muted" style="margin: 6px 0 0;">demo / demo123456</p>
        </div>
        <el-tabs v-model="mode">
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
          <el-form-item v-if="mode === 'register'" label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" :icon="LogIn" @click="submit">{{ mode === 'login' ? '登录' : '注册' }}</el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LogIn } from 'lucide-vue-next'
import { authApi } from '../api/http'

const router = useRouter()
const mode = ref('login')
const loading = ref(false)
const form = reactive({ username: 'demo', password: 'demo123456', email: '' })

async function submit() {
  loading.value = true
  try {
    const res = mode.value === 'login' ? await authApi.login(form) : await authApi.register(form)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data))
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>
