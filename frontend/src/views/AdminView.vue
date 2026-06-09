<template>
  <section class="config-layout">
    <ActionPanel eyebrow="Admin Access" title="管理员密钥" description="使用环境变量密钥访问设备额度控制台。">
      <template #action>
        <el-button type="primary" :icon="KeyRound" @click="loadDevices">连接后台</el-button>
      </template>
      <el-form label-position="top">
        <el-form-item label="管理员密钥">
          <el-input v-model="adminToken" type="password" show-password placeholder="输入 ADMIN_TOKEN" />
        </el-form-item>
        <el-form-item label="搜索设备">
          <el-input v-model="keyword" placeholder="按设备 ID 或备注搜索" @keyup.enter="loadDevices" />
        </el-form-item>
      </el-form>
    </ActionPanel>

    <ActionPanel eyebrow="Device Registry" title="设备白名单与额度" description="查看设备今日用量，调整每日额度、白名单和备注。">
      <EmptyState v-if="!devices.length" :icon="ShieldCheck" title="暂无设备数据" description="连接后台后会显示最近访问设备。" compact />
      <div v-else class="table-shell">
        <el-table :data="devices">
          <el-table-column prop="shortDeviceId" label="设备" width="110" />
          <el-table-column prop="deviceId" label="完整设备 ID" min-width="220" />
          <el-table-column prop="usedToday" label="今日已用" width="120" />
          <el-table-column label="每日额度" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.dailyLimit" :min="0" :max="5000000" :disabled="row.unlimitedQuota" />
            </template>
          </el-table-column>
          <el-table-column label="白名单" width="120">
            <template #default="{ row }">
              <el-switch v-model="row.whitelistEnabled" />
            </template>
          </el-table-column>
          <el-table-column label="不限额" width="120">
            <template #default="{ row }">
              <el-switch v-model="row.unlimitedQuota" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.note" placeholder="面试官设备 / 自己的电脑等" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button type="primary" size="small" @click="saveRow(row)">保存</el-button>
                <el-button size="small" @click="resetRow(row)">清零今日</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </ActionPanel>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { KeyRound, ShieldCheck } from 'lucide-vue-next'
import { adminDeviceApi, getAdminToken, setAdminToken } from '../api/http'
import ActionPanel from '../components/ActionPanel.vue'
import EmptyState from '../components/EmptyState.vue'

const adminToken = ref(getAdminToken())
const keyword = ref('')
const devices = ref([])

async function loadDevices() {
  setAdminToken(adminToken.value)
  const res = await adminDeviceApi.list(keyword.value)
  devices.value = res.data || []
}

async function saveRow(row) {
  setAdminToken(adminToken.value)
  const res = await adminDeviceApi.updateQuota(row.id, {
    dailyLimit: row.dailyLimit,
    whitelistEnabled: row.whitelistEnabled,
    unlimitedQuota: row.unlimitedQuota,
    note: row.note
  })
  Object.assign(row, res.data)
  ElMessage.success('设备额度已更新')
}

async function resetRow(row) {
  setAdminToken(adminToken.value)
  const res = await adminDeviceApi.resetToday(row.id)
  row.usedToday = res.data.usedToday
  row.remainingToday = res.data.remainingToday
  ElMessage.success('今日用量已清零')
}
</script>
