import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000, // AI 生成可能较慢
})

// Token 管理（持久化到 localStorage）
const TOKEN_KEY = 'satoken'

export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// 跟踪登录状态（由 user store 调用）
let hasLoggedIn = false

export function setLoggedIn() {
  hasLoggedIn = true
}

export function clearLoggedIn() {
  hasLoggedIn = false
}

// 请求拦截器：自动附加 token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.satoken = token
  }
  return config
})

// 认证相关接口（登录/注册时才提取 token）
const AUTH_PATHS = ['/v1/auth/login', '/v1/auth/register']

// 响应拦截器：提取并保存 token
request.interceptors.response.use(
  (response) => {
    // 仅在认证接口响应中提取 token，避免被其他接口意外覆盖
    const url = response.config.url || ''
    const isAuthRequest = AUTH_PATHS.some((path) => url.includes(path))
    if (isAuthRequest) {
      const token = response.headers['satoken'] || response.data?.data?.token
      if (token) {
        saveToken(token)
      }
    }

    const data = response.data
    if (data.code !== 200) {
      if (data.code === 401) {
        clearLoggedIn()
        clearToken()
        ElMessage.warning(hasLoggedIn ? '登录已过期，请重新登录' : '请先登录')
        router.push('/login')
      } else {
        ElMessage.error(data.message || '请求失败')
      }
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  (error) => {
    if (error.response?.status === 401) {
      clearLoggedIn()
      clearToken()
      ElMessage.warning(hasLoggedIn ? '登录已过期，请重新登录' : '请先登录')
      router.push('/login')
    } else {
      const serverMessage = error.response?.data?.message
      ElMessage.error(serverMessage || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
