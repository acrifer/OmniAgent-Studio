import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import AgentChatView from '../views/AgentChatView.vue'
import KnowledgeBaseView from '../views/KnowledgeBaseView.vue'
import ToolsView from '../views/ToolsView.vue'
import ModelsView from '../views/ModelsView.vue'
import StatsView from '../views/StatsView.vue'
import FeedbackView from '../views/FeedbackView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: DashboardView },
    { path: '/chat', component: AgentChatView },
    { path: '/knowledge', component: KnowledgeBaseView },
    { path: '/tools', component: ToolsView },
    { path: '/models', component: ModelsView },
    { path: '/stats', component: StatsView },
    { path: '/feedback', component: FeedbackView }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !localStorage.getItem('token')) return '/login'
})

export default router
