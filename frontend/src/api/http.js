import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
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

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data)
}

export const projectApi = {
  list: () => http.get('/projects'),
  create: (data) => http.post('/projects', data),
  detail: (id) => http.get(`/projects/${id}`),
  saveRequirement: (id, data) => http.put(`/projects/${id}/requirement`, data),
  uploadDocument: (id, file) => {
    const form = new FormData()
    form.append('file', file)
    return http.post(`/projects/${id}/documents`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  startTask: (id, data) => http.post(`/projects/${id}/agent-tasks`, data),
  latestTask: (id) => http.get(`/projects/${id}/agent-tasks/latest`),
  latestReport: (id) => http.get(`/projects/${id}/reports/latest`),
  archive: (id) => http.post(`/projects/${id}/archive`)
}

export const taskApi = {
  status: (id) => http.get(`/agent-tasks/${id}/status`),
  results: (id) => http.get(`/agent-tasks/${id}/results`),
  regenerate: (id, data) => http.post(`/agent-tasks/${id}/regenerate`, data)
}

export const promptApi = {
  list: () => http.get('/prompts'),
  create: (data) => http.post('/prompts', data)
}

export const statsApi = {
  tokens: (projectId) => http.get('/stats/tokens', { params: { projectId } })
}

export const feedbackApi = {
  create: (data) => http.post('/feedback', data)
}

export const assistantApi = {
  generateRequirementDraft: (data) => http.post('/ai/requirement-draft', data)
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
