import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/quiz/:sessionId',
      name: 'Quiz',
      component: () => import('@/views/Quiz.vue'),
    },
    {
      path: '/report/:sessionId',
      name: 'Report',
      component: () => import('@/views/Report.vue'),
    },
  ],
})

export default router
