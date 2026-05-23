<template>
  <div class="page-container">
    <!-- Loading -->
    <div v-if="loading" class="report-loading">
      <div class="spinner"></div>
      <p>{{ route.query.from === 'quiz' ? '正在生成学习报告...' : '正在加载学习报告...' }}</p>
    </div>

    <!-- 报告内容 -->
    <div v-else-if="report" class="report-page">
      <!-- 返回按钮 -->
      <div class="report-back" @click="route.query.from === 'quiz' ? router.push('/') : router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>

      <!-- 报告头部 -->
      <div class="report-header">
        <div class="report-emoji">
          <el-icon :size="48"><Target /></el-icon>
        </div>
        <div class="report-title-text">闯关完成！</div>
        <div class="report-topic">{{ report.title }}</div>
      </div>

      <!-- 统计卡片 -->
      <div class="report-stats">
        <div class="report-stat-card">
          <div class="report-stat-value">{{ report.score }}</div>
          <div class="report-stat-label">得分</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-value">{{ report.correctCount }}/{{ report.questionCount }}</div>
          <div class="report-stat-label">正确率</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-value">{{ formatDuration(report.durationSeconds) }}</div>
          <div class="report-stat-label">用时</div>
        </div>
      </div>

      <!-- 评级 -->
      <div class="report-rating">
        <div class="rating-stars">
          <el-icon v-for="i in starCount" :key="'s'+i" :size="24" class="star-filled">
            <Star />
          </el-icon>
          <el-icon v-for="i in (5 - starCount)" :key="'e'+i" :size="24" class="star-empty">
            <Star />
          </el-icon>
        </div>
        <div class="rating-title">
          <el-icon><Trophy /></el-icon>
          {{ report.rating }}
        </div>
      </div>

      <!-- 知识总结 -->
      <div class="report-section">
        <div class="report-section-title">
          <el-icon><Document /></el-icon>
          知识总结
        </div>
        <div class="report-summary">{{ report.knowledgeSummary }}</div>
      </div>

      <!-- 掌握情况 -->
      <div class="report-section">
        <div class="report-section-title">
          <el-icon><DataAnalysis /></el-icon>
          掌握情况
        </div>
        <div class="mastery-list">
          <div v-for="point in report.strengthPoints" :key="point" class="mastery-item good">
            <el-icon><CircleCheck /></el-icon>
            {{ point }}
          </div>
          <div v-for="point in report.weakPoints" :key="point" class="mastery-item bad">
            <el-icon><CircleClose /></el-icon>
            {{ point }}
          </div>
        </div>
      </div>

      <!-- 错题回顾 -->
      <div v-if="report.wrongQuestions && report.wrongQuestions.length > 0" class="report-section">
        <div class="report-section-title">
          <el-icon><CircleClose /></el-icon>
          错题回顾
        </div>
        <div v-for="wq in report.wrongQuestions" :key="wq.questionId" class="wrong-question">
          <div class="wrong-question-text">
            第 {{ wq.questionIndex }} 题：{{ wq.questionContent }}
          </div>
          <div class="wrong-question-answers">
            <span class="correct">正确答案：{{ wq.correctAnswer }}</span>
            <span class="user">你的答案：{{ wq.userAnswer }}</span>
          </div>
        </div>
      </div>

      <!-- 完整答题记录 -->
      <div v-if="questionDetails.length > 0" class="report-section">
        <div class="report-section-title">
          <el-icon><Document /></el-icon>
          答题记录
        </div>
        <div v-for="q in questionDetails" :key="q.questionId" class="detail-question">
          <div class="detail-question-header">
            <span class="detail-question-index">第 {{ q.questionIndex }} 题</span>
            <span v-if="q.isCorrect === 1" class="detail-status correct">回答正确</span>
            <span v-else-if="q.isCorrect === 0" class="detail-status wrong">回答错误</span>
            <span v-else class="detail-status neutral">未作答</span>
          </div>
          <div class="detail-question-text">{{ q.questionContent }}</div>
          <div class="detail-options">
            <div :class="['detail-option', getOptionClass('A', q)]">
              <span class="detail-option-letter">A</span>
              <span>{{ q.optionA }}</span>
            </div>
            <div :class="['detail-option', getOptionClass('B', q)]">
              <span class="detail-option-letter">B</span>
              <span>{{ q.optionB }}</span>
            </div>
            <div :class="['detail-option', getOptionClass('C', q)]">
              <span class="detail-option-letter">C</span>
              <span>{{ q.optionC }}</span>
            </div>
            <div :class="['detail-option', getOptionClass('D', q)]">
              <span class="detail-option-letter">D</span>
              <span>{{ q.optionD }}</span>
            </div>
          </div>
          <div class="detail-answers">
            <span v-if="q.userAnswer" class="detail-answer-item">
              你的答案：<span :class="q.isCorrect === 1 ? 'text-correct' : 'text-wrong'">{{ q.userAnswer }}</span>
            </span>
            <span class="detail-answer-item">
              正确答案：<span class="text-correct">{{ q.correctAnswer }}</span>
            </span>
          </div>
          <div v-if="q.explanation" class="detail-explanation">
            <strong>解析：</strong>{{ q.explanation }}
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="report-actions">
        <el-button type="primary" class="btn-report primary" @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button class="btn-report secondary" @click="handleRetryWrong" disabled>
          <el-icon><RefreshRight /></el-icon>
          错题重练
        </el-button>
        <el-button class="btn-report secondary" disabled>
          <el-icon><Share /></el-icon>
          分享报告
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReport, getQuestionDetails } from '@/api/quiz'
import type { ReportVO, QuestionDetailVO } from '@/api/quiz'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const report = ref<ReportVO | null>(null)
const questionDetails = ref<QuestionDetailVO[]>([])

const sessionId = computed(() => Number(route.params.sessionId))

const starCount = computed(() => {
  if (!report.value) return 0
  const score = report.value.score
  if (score >= 90) return 5
  if (score >= 80) return 4
  if (score >= 60) return 3
  if (score >= 40) return 2
  return 1
})

function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return "0'00\""
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}'${String(s).padStart(2, '0')}"`
}

function handleRetryWrong() {
  ElMessage.info('错题重练功能即将上线')
}

function getOptionClass(option: string, q: QuestionDetailVO) {
  if (option === q.correctAnswer) return 'correct'
  if (option === q.userAnswer && q.isCorrect === 0) return 'wrong'
  return 'neutral'
}

onMounted(async () => {
  try {
    const [reportRes, detailsRes] = await Promise.all([
      getReport(sessionId.value),
      getQuestionDetails(sessionId.value)
    ])
    report.value = reportRes
    questionDetails.value = detailsRes
  } catch (e) {
    ElMessage.error('获取报告失败')
    router.push('/')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.report-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
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

.report-page {
  padding: 48px 40px;
  max-width: 800px;
  margin: 0 auto;
}

.report-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  margin-bottom: 24px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.report-back:hover {
  color: var(--primary);
  background: rgba(108, 92, 231, 0.06);
}

.report-header {
  text-align: center;
  margin-bottom: 40px;
}

.report-emoji {
  margin-bottom: 12px;
  color: var(--primary);
}

.report-title-text {
  font-size: 14px;
  color: var(--text-light);
  margin-bottom: 8px;
}

.report-topic {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.report-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  max-width: 560px;
  margin: 0 auto 32px;
}

.report-stat-card {
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 24px;
  text-align: center;
}

.report-stat-value {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary), var(--success));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 4px;
}

.report-stat-label {
  font-size: 13px;
  color: var(--text-light);
}

.report-rating {
  text-align: center;
  margin-bottom: 40px;
}

.rating-stars {
  margin-bottom: 8px;
}

.star-filled {
  color: var(--warning);
  fill: var(--warning);
}

.star-empty {
  color: var(--border);
}

.rating-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 20px;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.1), rgba(0, 210, 211, 0.1));
  border-radius: 20px;
  font-size: 15px;
  font-weight: 700;
  color: var(--primary);
}

.report-section {
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 24px;
  margin-bottom: 20px;
}

.report-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-summary {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.mastery-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mastery-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.mastery-item.good {
  color: var(--success);
}

.mastery-item.bad {
  color: var(--danger);
}

.wrong-question {
  padding: 16px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.wrong-question:last-child {
  margin-bottom: 0;
}

.wrong-question-text {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.wrong-question-answers {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.wrong-question-answers .correct {
  color: var(--success);
  font-weight: 600;
}

.wrong-question-answers .user {
  color: var(--danger);
  font-weight: 600;
}

.report-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.btn-report {
  padding: 14px 28px !important;
  border-radius: var(--radius) !important;
  font-weight: 600 !important;
}

.btn-report.primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-light)) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(108, 92, 231, 0.2) !important;
}

.btn-report.secondary {
  background: white !important;
  color: var(--text-secondary) !important;
  border: 1px solid var(--border) !important;
}

.detail-question {
  padding: 16px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.detail-question:last-child {
  margin-bottom: 0;
}

.detail-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-question-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--primary);
}

.detail-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.detail-status.correct {
  background: var(--success-light);
  color: var(--success);
}

.detail-status.wrong {
  background: var(--danger-light);
  color: var(--danger);
}

.detail-status.neutral {
  background: var(--border);
  color: var(--text-light);
}

.detail-question-text {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
  margin-bottom: 12px;
}

.detail-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.detail-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.detail-option.correct {
  background: var(--success-light);
  color: var(--success);
  font-weight: 600;
}

.detail-option.wrong {
  background: var(--danger-light);
  color: var(--danger);
  text-decoration: line-through;
}

.detail-option.neutral {
  background: white;
  color: var(--text-secondary);
}

.detail-option-letter {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.detail-option.correct .detail-option-letter {
  background: var(--success);
  color: white;
}

.detail-option.wrong .detail-option-letter {
  background: var(--danger);
  color: white;
}

.detail-option.neutral .detail-option-letter {
  background: var(--border);
  color: var(--text-secondary);
}

.detail-answers {
  display: flex;
  gap: 16px;
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-answer-item {
  font-weight: 500;
}

.text-correct {
  color: var(--success);
  font-weight: 700;
}

.text-wrong {
  color: var(--danger);
  font-weight: 700;
}

.detail-explanation {
  background: white;
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.detail-explanation strong {
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .report-page { padding: 32px 20px; }
  .report-stats { gap: 12px; }
  .report-stat-value { font-size: 24px; }
  .report-actions { flex-direction: column; }
}
</style>
