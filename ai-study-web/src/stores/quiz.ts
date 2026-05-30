import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { QuestionVO, AnswerResult } from '@/api/quiz'

export const useQuizStore = defineStore('quiz', () => {
  const sessionId = ref<number | null>(null)
  const title = ref('')
  const questions = ref<QuestionVO[]>([])
  const currentIndex = ref(0)
  const answers = ref<Map<number, AnswerResult>>(new Map())
  const streak = ref(0)
  const totalPoints = ref(0)
  const selectedAnswer = ref<string | null>(null)
  const showFeedback = ref(false)
  const currentResult = ref<AnswerResult | null>(null)

  function initQuiz(sid: number, t: string, qs: QuestionVO[]) {
    sessionId.value = sid
    title.value = t
    questions.value = qs
    currentIndex.value = 0
    answers.value = new Map()
    streak.value = 0
    totalPoints.value = 0
    selectedAnswer.value = null
    showFeedback.value = false
    currentResult.value = null
  }

  function setAnswer(questionId: number, result: AnswerResult) {
    answers.value.set(questionId, result)
    streak.value = result.streak
    totalPoints.value += result.points
    currentResult.value = result
    showFeedback.value = true
  }

  function nextQuestion() {
    currentIndex.value++
    selectedAnswer.value = null
    showFeedback.value = false
    currentResult.value = null
  }

  function reset() {
    sessionId.value = null
    title.value = ''
    questions.value = []
    currentIndex.value = 0
    answers.value = new Map()
    streak.value = 0
    totalPoints.value = 0
    selectedAnswer.value = null
    showFeedback.value = false
    currentResult.value = null
  }

  return {
    sessionId, title, questions, currentIndex,
    answers, streak, totalPoints, selectedAnswer, showFeedback, currentResult,
    initQuiz, setAnswer, nextQuestion, reset,
  }
})
