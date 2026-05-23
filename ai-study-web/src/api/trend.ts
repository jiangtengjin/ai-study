import request from './request'

export interface TrendStats {
  date: string
  totalQuestions: number
  correctAnswers: number
  accuracy: number
  studyDuration: number
}

export interface TrendStatsParams {
  period?: 'day' | 'week' | 'month'
  days?: 7 | 30 | 90
}

// 获取学习趋势数据
export function getTrendStats(params?: TrendStatsParams) {
  return request.get<TrendStats[]>('/v1/trend/stats', { params })
}
