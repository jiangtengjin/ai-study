import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo, type UserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref(false)

  const nickname = computed(() => userInfo.value?.nickname || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const email = computed(() => userInfo.value?.email || '')

  async function fetchUserInfo() {
    try {
      const data = await getUserInfo()
      userInfo.value = data
      isLoggedIn.value = true
    } catch {
      userInfo.value = null
      isLoggedIn.value = false
    }
  }

  function setUser(info: UserInfo) {
    userInfo.value = info
    isLoggedIn.value = true
  }

  function clearUser() {
    userInfo.value = null
    isLoggedIn.value = false
  }

  return {
    userInfo,
    isLoggedIn,
    nickname,
    avatar,
    email,
    fetchUserInfo,
    setUser,
    clearUser
  }
})
