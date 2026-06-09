import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import AgentChatView from '../views/AgentChatView.vue'
import KnowledgeBaseView from '../views/KnowledgeBaseView.vue'
import ToolsView from '../views/ToolsView.vue'
import ModelsView from '../views/ModelsView.vue'
import StatsView from '../views/StatsView.vue'
import FeedbackView from '../views/FeedbackView.vue'
import AdminView from '../views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: DashboardView },
    { path: '/chat', component: AgentChatView },
    { path: '/knowledge', component: KnowledgeBaseView },
    { path: '/tools', component: ToolsView },
    { path: '/models', component: ModelsView },
    { path: '/stats', component: StatsView },
    { path: '/feedback', component: FeedbackView },
    { path: '/admin', component: AdminView }
  ]
})

export default router
