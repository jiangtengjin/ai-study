<template>
  <div class="callback-page">
    <div class="callback-content">
      <el-icon :size="48" class="loading-icon"><Loading /></el-icon>
      <div class="callback-text">正在完成登录...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { githubCallback } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

onMounted(async () => {
  const code = route.query.code as string

  if (!code) {
    ElMessage.error('授权失败：缺少授权码')
    router.push('/login')
    return
  }

  // 如果后端已经处理了回调并重定向到这里
  if (code === 'success') {
    // 获取用户信息
    try {
      await userStore.fetchUserInfo()
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error) {
      ElMessage.error('获取用户信息失败')
      router.push('/login')
    }
    return
  }

  // 前端直接处理回调（备用方案）
  try {
    const data = await githubCallback(code)
    userStore.setUser(data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('GitHub 登录失败:', error)
    ElMessage.error('登录失败，请重试')
    router.push('/login')
  }
})
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
}

.callback-content {
  text-align: center;
}

.loading-icon {
  color: var(--primary);
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.callback-text {
  margin-top: 16px;
  font-size: 16px;
  color: var(--text-secondary);
}
</style>
