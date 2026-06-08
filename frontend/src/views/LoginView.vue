<template>
  <div class="login-page">
    <div class="login-card">
      <p class="page-kicker">OmniAgent Studio</p>
      <h1>通用多模态智能体平台</h1>
      <p class="subtitle">提问、上传资料、连接知识库或工具，由多个内部 Agent 协作完成搜索、阅读、检索、校验和最终回答。</p>
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
