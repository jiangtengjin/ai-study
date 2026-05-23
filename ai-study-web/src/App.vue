<template>
  <div class="app-container">
    <!-- Navigation Bar -->
    <nav class="navbar" v-if="!isLoginRoute">
      <div class="navbar-content">
        <router-link to="/" class="navbar-brand">
          <div class="navbar-brand-icon">AI</div>
          <span class="navbar-brand-text">知识闯关</span>
        </router-link>

        <div class="navbar-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click">
              <div class="navbar-user">
                <div class="navbar-avatar">
                  {{ userStore.nickname?.charAt(0) || '?' }}
                </div>
                <span class="navbar-nickname">{{ userStore.nickname }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">
                    <el-icon><User /></el-icon> 个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/profile/history')">
                    <el-icon><Clock /></el-icon> 历史记录
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/profile/wrong-book')">
                    <el-icon><Document /></el-icon> 错题本
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon> 退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main :class="['main-content', { 'has-navbar': !isLoginRoute }]">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Clock, Document, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLoginRoute = computed(() => route.path === '/login')

let loginCheckTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  // 路由守卫未初始化时才主动获取用户信息
  if (!userStore.isLoggedIn) {
    await userStore.fetchUserInfo()
  }

  // 每 5 分钟检查一次登录状态
  loginCheckTimer = setInterval(async () => {
    if (userStore.isLoggedIn) {
      await userStore.fetchUserInfo()
      if (!userStore.isLoggedIn) {
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login')
      }
    }
  }, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (loginCheckTimer) {
    clearInterval(loginCheckTimer)
  }
})

async function handleLogout() {
  try {
    await logout()
    userStore.clearUser()
    ElMessage.success('已退出登录')
    router.push('/')
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: var(--bg);
}

.navbar {
  background: white;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
}

.navbar-brand-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: white;
}

.navbar-brand-text {
  font-size: 18px;
  font-weight: 700;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.navbar-user:hover {
  background: var(--bg);
}

.navbar-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: white;
}

.navbar-nickname {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.main-content.has-navbar {
  padding-top: 0;
}
</style>
