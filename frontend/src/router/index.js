import { createRouter, createWebHistory } from 'vue-router'
import Landing from '@/views/Landing.vue'
import Home from '@/views/Home.vue'
import Chat from '@/views/Chat.vue'
import Workflow from '@/views/Workflow.vue'

const routes = [
  { path: '/', name: 'Landing', component: Landing },
  { path: '/translate', name: 'Home', component: Home },
  { path: '/chat', name: 'Chat', component: Chat },
  { path: '/workflow', name: 'Workflow', component: Workflow }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
