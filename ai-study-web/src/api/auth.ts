import request from './request'

export interface RegisterData {
  email: string
  password: string
  nickname?: string
  captcha: string
}

export interface LoginData {
  email: string
  password: string
  captcha: string
}

export interface UserInfo {
  id: number
  nickname: string
  avatar: string | null
  email: string
  vipLevel: number
  totalQuizzes: number
  totalCorrect: number
  totalQuestions: number
  streakDays: number
  lastStudyDate: string | null
  createdAt: string
}

// 获取验证码
export function getCaptcha() {
  return request.get<{ image: string }>('/v1/captcha')
}

// 邮箱注册
export function register(data: RegisterData) {
  return request.post<UserInfo>('/v1/auth/register', data)
}

// 邮箱登录
export function login(data: LoginData) {
  return request.post<UserInfo>('/v1/auth/login', data)
}

// 退出登录
export function logout() {
  return request.post('/v1/auth/logout')
}

// 获取登录状态
export function getLoginStatus() {
  return request.get<boolean>('/v1/auth/status')
}

// 获取当前用户信息
export function getUserInfo() {
  return request.get<UserInfo>('/v1/user/info')
}

// 更新用户信息
export function updateUserInfo(data: { nickname?: string; avatar?: string }) {
  return request.put('/v1/user/info', data)
}

// 获取 GitHub 授权 URL
export function getGithubAuthUrl() {
  return request.get<{ url: string }>('/v1/auth/github')
}

// GitHub OAuth 回调
export function githubCallback(code: string) {
  return request.get<UserInfo>('/v1/auth/github/callback', { params: { code } })
}
