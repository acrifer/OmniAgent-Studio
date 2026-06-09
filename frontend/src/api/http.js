import axios from 'axios'

const DEVICE_ID_KEY = 'omni_device_id'
const ADMIN_TOKEN_KEY = 'omni_admin_token'

function randomPart() {
  return Math.random().toString(36).slice(2, 10)
}

export function ensureDeviceId() {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY)
  if (!deviceId) {
    deviceId = `${randomPart()}-${randomPart()}`
    localStorage.setItem(DEVICE_ID_KEY, deviceId)
  }
  return deviceId
}

export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export function setAdminToken(token) {
  if (!token) {
    localStorage.removeItem(ADMIN_TOKEN_KEY)
    return
  }
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  config.headers['X-Device-Id'] = ensureDeviceId()
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(body)
    }
    return body
  },
  (error) => Promise.reject(error.response?.data || error)
)

export const adminHttp = axios.create({
  baseURL: '/api/admin',
  timeout: 60000
})

adminHttp.interceptors.request.use((config) => {
  config.headers['X-Admin-Token'] = getAdminToken()
  return config
})

adminHttp.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(body)
    }
    return body
  },
  (error) => Promise.reject(error.response?.data || error)
)

export const feedbackApi = {
  create: (data) => http.post('/feedback', data)
}

export const conversationApi = {
  list: () => http.get('/conversations'),
  create: (data) => http.post('/conversations', data),
  detail: (id) => http.get(`/conversations/${id}`),
  sendMessage: (id, data) => http.post(`/conversations/${id}/messages`, data),
  uploadFile: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post(`/conversations/${id}/files`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const agentRunApi = {
  start: (data) => http.post('/agent-runs', data),
  status: (id) => http.get(`/agent-runs/${id}/status`),
  steps: (id) => http.get(`/agent-runs/${id}/steps`),
  answer: (id) => http.get(`/agent-runs/${id}/answer`)
}

export const knowledgeApi = {
  list: () => http.get('/knowledge-bases'),
  create: (data) => http.post('/knowledge-bases', data),
  documents: (id) => http.get(`/knowledge-bases/${id}/documents`),
  uploadDocument: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post(`/knowledge-bases/${id}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const modelProviderApi = {
  list: () => http.get('/model-providers'),
  save: (data) => http.post('/model-providers', data)
}

export const toolApi = {
  list: () => http.get('/tools'),
  save: (data) => http.post('/tools', data)
}

export const skillApi = {
  list: () => http.get('/skills'),
  save: (data) => http.post('/skills', data)
}

export const omniStatsApi = {
  tokens: (conversationId) => http.get('/omni/stats/tokens', { params: { conversationId } })
}

export const deviceApi = {
  quota: () => http.get('/device/quota')
}

export const adminDeviceApi = {
  list: (keyword) => adminHttp.get('/devices', { params: keyword ? { keyword } : {} }),
  updateQuota: (id, data) => adminHttp.patch(`/devices/${id}/quota`, data),
  resetToday: (id) => adminHttp.post(`/devices/${id}/reset-today`)
}
