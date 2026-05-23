import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

let userInitialized = false

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/oauth/callback',
      name: 'OAuthCallback',
      component: () => import('@/views/OAuthCallback.vue'),
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
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/history',
      name: 'History',
      component: () => import('@/views/History.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/wrong-book',
      name: 'WrongBook',
      component: () => import('@/views/WrongBook.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/reports',
      name: 'Reports',
      component: () => import('@/views/Reports.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile/settings',
      name: 'Settings',
      component: () => import('@/views/Settings.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userInitialized) {
      await userStore.fetchUserInfo()
      userInitialized = true
    }
    if (!userStore.isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
