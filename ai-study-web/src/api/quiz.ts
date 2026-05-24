import request from './request'

export interface CreateQuizParams {
  content: string
  questionCount: number
  difficulty: string
  enableSearch: boolean
  knowledgeBaseId?: number
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
  score: number
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

export interface QuestionDetailVO {
  questionId: number
  questionIndex: number
  questionContent: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctAnswer: string
  userAnswer: string | null
  isCorrect: number | null
  explanation: string
  knowledgePoint: string
  answerTimeSeconds: number | null
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
  return request.post<CreateQuizResult>('/v1/quiz/create', data)
}

// 获取所有题目
export function getQuestions(sessionId: number) {
  return request.get<QuestionVO[]>(`/v1/quiz/${sessionId}/questions`)
}

// 提交答案
export function submitAnswer(sessionId: number, data: {
  questionId: number
  userAnswer: string
  answerTimeSeconds: number
}) {
  return request.post<AnswerResult>(`/v1/quiz/${sessionId}/answer`, data)
}

// 结束答题
export function finishSession(sessionId: number) {
  return request.post<void>(`/v1/quiz/${sessionId}/finish`)
}

// 获取学习报告
export function getReport(sessionId: number) {
  return request.get<ReportVO>(`/v1/report/${sessionId}`)
}

// 获取完整答题记录（含用户答案和解析）
export function getQuestionDetails(sessionId: number) {
  return request.get<QuestionDetailVO[]>(`/v1/quiz/${sessionId}/detail`)
}

// 重新练习：基于原会话创建新会话
export function retrySession(sessionId: number) {
  return request.post<CreateQuizResult>(`/v1/quiz/${sessionId}/retry`)
}
