import request from './request'

export interface ProfileStats {
  totalQuizzes: number
  totalQuestions: number
  totalCorrect: number
  correctRate: number
  streakDays: number
  averageScore: number
}

export interface HistoryItem {
  sessionId: number
  title: string
  questionCount: number
  correctCount: number
  score: number
  durationSeconds: number
  difficulty: string
  createdAt: string
}

export interface WrongQuestion {
  answerId: number
  sessionId: number
  sessionTitle: string | null
  questionId: number
  questionIndex: number
  questionContent: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: string
  userAnswer: string
  explanation: string
  knowledgePoint: string
  createdAt: string
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
}

// 获取学习统计
export function getProfileStats() {
  return request.get<ProfileStats>('/v1/profile/stats')
}

// 获取历史记录
export function getHistory(params: { page?: number; size?: number; filter?: string }) {
  return request.get<PageData<HistoryItem>>('/v1/profile/history', { params })
}

// 获取错题本
export function getWrongQuestions(params: { page?: number; size?: number }) {
  return request.get<PageData<WrongQuestion>>('/v1/profile/wrong-questions', { params })
}

// 移除错题
export function removeWrongQuestion(answerId: number) {
  return request.delete(`/v1/profile/wrong-questions/${answerId}`)
}
