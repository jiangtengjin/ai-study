import request from './request'

export interface CreateQuizParams {
  content: string
  questionCount: number
  difficulty: string
}

export interface CreateQuizResult {
  sessionId: number
  title: string
  questionCount: number
  status: string
}

export interface QuestionVO {
  id: number
  index: number
  questionType: string
  difficulty: string
  questionContent: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
}

export interface AnswerResult {
  isCorrect: boolean
  correctAnswer: string
  explanation: string
  knowledgePoint: string
  currentProgress: number
  totalQuestions: number
  streak: number
}

export interface WrongQuestionVO {
  questionId: number
  questionIndex: number
  questionContent: string
  correctAnswer: string
  userAnswer: string
  explanation: string
}

export interface ReportVO {
  sessionId: number
  title: string
  score: number
  correctCount: number
  questionCount: number
  durationSeconds: number
  rating: string
  knowledgeSummary: string
  wrongQuestions: WrongQuestionVO[]
  strengthPoints: string[]
  weakPoints: string[]
}

// 创建答题会话
export function createQuiz(data: CreateQuizParams) {
  return request.post<any, { data: CreateQuizResult }>('/v1/quiz/create', data)
}

// 获取所有题目
export function getQuestions(sessionId: number) {
  return request.get<any, { data: QuestionVO[] }>(`/v1/quiz/${sessionId}/questions`)
}

// 提交答案
export function submitAnswer(sessionId: number, data: {
  questionId: number
  userAnswer: string
  answerTimeSeconds: number
}) {
  return request.post<any, { data: AnswerResult }>(`/v1/quiz/${sessionId}/answer`, data)
}

// 结束答题
export function finishSession(sessionId: number) {
  return request.post<any, { data: null }>(`/v1/quiz/${sessionId}/finish`)
}

// 获取学习报告
export function getReport(sessionId: number) {
  return request.get<any, { data: ReportVO }>(`/v1/report/${sessionId}`)
}
