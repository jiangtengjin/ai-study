import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000, // AI 生成可能较慢
})

let hasToken = false

// 请求拦截器：记录是否曾携带 token
request.interceptors.request.use((config) => {
  if (config.headers?.satoken) {
    hasToken = true
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code !== 200) {
      if (data.code === 401) {
        if (hasToken) {
          ElMessage.warning('登录已过期，请重新登录')
        } else {
          ElMessage.warning('请先登录')
        }
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
      if (hasToken) {
        ElMessage.warning('登录已过期，请重新登录')
      } else {
        ElMessage.warning('请先登录')
      }
      router.push('/login')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
