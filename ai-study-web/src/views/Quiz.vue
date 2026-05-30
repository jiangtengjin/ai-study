<template>
  <div class="page-container">
    <!-- 顶部导航 -->
    <div class="quiz-nav">
      <div class="quiz-back" @click="handleBack">← 返回</div>
      <div class="quiz-title">{{ quizStore.title }}</div>
      <div class="quiz-stats">
        <div class="quiz-stat">
          <el-icon><DataAnalysis /></el-icon>
          <span>进度 {{ currentIndex + 1 }}/{{ totalQuestions }}</span>
        </div>
        <div class="quiz-stat">
          <el-icon><Flame /></el-icon>
          <span>连对 {{ quizStore.streak }}</span>
        </div>
        <div class="quiz-stat points-stat">
          <el-icon><Coin /></el-icon>
          <span>{{ quizStore.totalPoints }} 积分</span>
        </div>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="quiz-progress-bar">
      <div
        class="quiz-progress-fill"
        :style="{ width: progressPercent + '%' }"
      ></div>
    </div>

    <!-- Loading 状态 -->
    <div v-if="loadingQuestions" class="quiz-loading">
      <div class="spinner"></div>
      <p>加载题目中...</p>
    </div>

    <!-- 题目内容 -->
    <div v-else-if="currentQuestion" class="quiz-content">
      <div class="quiz-question-header">
        <span class="quiz-question-num">第 {{ currentIndex + 1 }} 题</span>
        <span class="quiz-difficulty" :class="currentQuestion.difficulty">
          {{ difficultyLabel(currentQuestion.difficulty) }}
        </span>
        <span class="quiz-point" v-if="currentQuestion.score">{{ currentQuestion.score }} 分</span>
      </div>

      <div class="quiz-question-text">
        {{ currentQuestion.questionContent }}
      </div>

      <!-- 选项列表 -->
      <div class="quiz-options">
        <div
          v-for="letter in ['A', 'B', 'C', 'D']"
          :key="letter"
          class="quiz-option"
          :class="getOptionClass(letter)"
          @click="handleSelectOption(letter)"
        >
          <div class="option-letter">{{ letter }}</div>
          <div class="option-text">{{ getOptionText(letter) }}</div>
        </div>
      </div>

      <!-- 反馈区域 -->
      <Transition name="fade">
        <div v-if="quizStore.showFeedback && quizStore.currentResult" class="quiz-feedback"
             :class="quizStore.currentResult.isCorrect ? 'correct' : 'wrong'">
          <div class="feedback-header" :class="quizStore.currentResult.isCorrect ? 'correct' : 'wrong'">
            <el-icon v-if="quizStore.currentResult.isCorrect"><CircleCheck /></el-icon>
            <el-icon v-else><CircleClose /></el-icon>
            {{ quizStore.currentResult.isCorrect ? '答对了！' : '答错了' }}
            <span class="feedback-points" v-if="quizStore.currentResult.points">+{{ quizStore.currentResult.points }} 积分</span>
          </div>
          <div class="feedback-text">
            <template v-if="!quizStore.currentResult.isCorrect">
              <strong>正确答案：{{ quizStore.currentResult.correctAnswer }}</strong><br><br>
            </template>
            <strong>解析：</strong>{{ quizStore.currentResult.explanation }}
          </div>
        </div>
      </Transition>
    </div>

    <!-- 底部按钮 -->
    <div v-if="quizStore.showFeedback" class="quiz-footer">
      <el-button type="primary" class="btn-next" @click="handleNext">
        {{ isLastQuestion ? '查看报告' : '下一题 →' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useQuizStore } from '@/stores/quiz'
import { getQuestions, submitAnswer, finishSession } from '@/api/quiz'
import type { QuestionVO } from '@/api/quiz'

const route = useRoute()
const router = useRouter()
const quizStore = useQuizStore()
const loadingQuestions = ref(false)
const startTime = ref(Date.now())

const sessionId = computed(() => Number(route.params.sessionId))
const currentIndex = computed(() => quizStore.currentIndex)
const totalQuestions = computed(() => quizStore.questions.length)
const currentQuestion = computed<QuestionVO | null>(() =>
  quizStore.questions[currentIndex.value] || null
)
const isLastQuestion = computed(() => currentIndex.value >= totalQuestions.value - 1)
const progressPercent = computed(() =>
  totalQuestions.value > 0
    ? ((currentIndex.value + (quizStore.showFeedback ? 1 : 0)) / totalQuestions.value) * 100
    : 0
)

function difficultyLabel(d: string) {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难', balanced: '均衡' }
  return map[d] || d
}

function getOptionText(letter: string) {
  const q = currentQuestion.value
  if (!q) return ''
  const map: Record<string, string> = { A: q.optionA, B: q.optionB, C: q.optionC, D: q.optionD }
  return map[letter] || ''
}

function getOptionClass(letter: string) {
  const classes: string[] = []
  if (quizStore.selectedAnswer === letter) {
    classes.push('selected')
  }
  if (quizStore.showFeedback && quizStore.currentResult) {
    const result = quizStore.currentResult
    if (letter === result.correctAnswer) {
      classes.push('correct')
    } else if (letter === quizStore.selectedAnswer && !result.isCorrect) {
      classes.push('wrong')
    }
  }
  return classes.join(' ')
}

async function handleSelectOption(letter: string) {
  if (quizStore.showFeedback) return // 已提交过
  if (!currentQuestion.value) return

  quizStore.selectedAnswer = letter
  const questionStartTime = startTime.value

  try {
    const res = await submitAnswer(sessionId.value, {
      questionId: currentQuestion.value.id,
      userAnswer: letter,
      answerTimeSeconds: Math.floor((Date.now() - questionStartTime) / 1000),
    })
    quizStore.setAnswer(currentQuestion.value.id, res)
  } catch (e) {
    quizStore.selectedAnswer = null
  }
}

async function handleNext() {
  if (isLastQuestion.value) {
    // 最后一题 → 结束答题 → 跳转报告
    try {
      await finishSession(sessionId.value)
      quizStore.reset()
      router.push(`/report/${sessionId.value}?from=quiz`)
    } catch (e) {
      // 错误已处理
    }
  } else {
    quizStore.nextQuestion()
    startTime.value = Date.now()
  }
}

async function handleBack() {
  try {
    await ElMessageBox.confirm('确定要退出答题吗？当前进度将不会保存。', '提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '继续答题',
      type: 'warning',
    })
    quizStore.reset()
    router.push('/')
  } catch {
    // 取消
  }
}

onMounted(async () => {
  // 如果 store 中没有数据（比如直接访问 URL），则从后端加载
  if (quizStore.questions.length === 0 || quizStore.sessionId !== sessionId.value) {
    loadingQuestions.value = true
    try {
      const res = await getQuestions(sessionId.value)
      quizStore.initQuiz(sessionId.value, '知识闯关', res)
    } catch (e) {
      ElMessage.error('加载题目失败')
      router.push('/')
      return
    } finally {
      loadingQuestions.value = false
    }
  }
  startTime.value = Date.now()
})
</script>

<style scoped>
.quiz-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  background: white;
  border-bottom: 1px solid var(--border);
}

.quiz-back {
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.quiz-back:hover {
  color: var(--primary);
}

.quiz-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.quiz-stats {
  display: flex;
  align-items: center;
  gap: 20px;
}

.quiz-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.quiz-stat.points-stat {
  color: var(--primary);
  font-weight: 600;
}

.quiz-progress-bar {
  height: 6px;
  background: var(--border);
}

.quiz-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--success));
  border-radius: 0 3px 3px 0;
  transition: width 0.5s ease;
}

.quiz-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 16px;
  color: var(--text-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.quiz-content {
  padding: 48px 40px;
  max-width: 720px;
  margin: 0 auto;
}

.quiz-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.quiz-question-num {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary);
}

.quiz-difficulty {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.quiz-difficulty.easy {
  background: var(--success-light);
  color: var(--success);
}

.quiz-difficulty.medium {
  background: var(--warning-light);
  color: #F39C12;
}

.quiz-difficulty.hard {
  background: var(--danger-light);
  color: var(--danger);
}

.quiz-point {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(108, 92, 231, 0.08);
  color: var(--primary);
}

.quiz-question-text {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.6;
  margin-bottom: 32px;
  color: var(--text-primary);
}

.quiz-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quiz-option {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  background: white;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
}

.quiz-option:hover:not(.correct):not(.wrong) {
  border-color: var(--primary-light);
  background: rgba(108, 92, 231, 0.02);
}

.quiz-option.selected {
  border-color: var(--primary);
  background: rgba(108, 92, 231, 0.04);
}

.quiz-option.correct {
  border-color: var(--success);
  background: var(--success-light);
  cursor: default;
}

.quiz-option.wrong {
  border-color: var(--danger);
  background: var(--danger-light);
  cursor: default;
}

.option-letter {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  background: var(--bg);
  color: var(--text-secondary);
  flex-shrink: 0;
}

.quiz-option.selected .option-letter {
  background: var(--primary);
  color: white;
}

.quiz-option.correct .option-letter {
  background: var(--success);
  color: white;
}

.quiz-option.wrong .option-letter {
  background: var(--danger);
  color: white;
}

.option-text {
  font-size: 15px;
  line-height: 1.6;
  padding-top: 4px;
}

.quiz-feedback {
  margin-top: 24px;
  padding: 20px 24px;
  border-radius: var(--radius);
}

.quiz-feedback.correct {
  background: var(--success-light);
  border: 1px solid rgba(0, 210, 211, 0.2);
}

.quiz-feedback.wrong {
  background: var(--danger-light);
  border: 1px solid rgba(255, 107, 107, 0.2);
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
}

.feedback-header.correct {
  color: var(--success);
}

.feedback-header.wrong {
  color: var(--danger);
}

.feedback-points {
  margin-left: auto;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  background: rgba(108, 92, 231, 0.1);
  color: var(--primary);
}

.feedback-text {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.feedback-text strong {
  color: var(--text-primary);
}

.quiz-footer {
  display: flex;
  justify-content: flex-end;
  padding: 20px 40px;
  border-top: 1px solid var(--border);
  max-width: 800px;
  margin: 0 auto;
}

.btn-next {
  padding: 12px 28px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  border-radius: var(--radius) !important;
  background: linear-gradient(135deg, var(--primary), var(--primary-light)) !important;
  border: none !important;
}

.fade-enter-active {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .quiz-nav { padding: 12px 20px; }
  .quiz-content { padding: 32px 20px; }
  .quiz-question-text { font-size: 17px; }
}
</style>
