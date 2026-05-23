<template>
  <div class="reports-page">
    <!-- Nav -->
    <div class="page-nav">
      <div class="page-nav-back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">学习报告</div>
      <div class="page-nav-action"></div>
    </div>

    <!-- Summary -->
    <div class="reports-summary">
      <div class="reports-stat">
        <div class="reports-stat-value">{{ stats.totalQuizzes }}</div>
        <div class="reports-stat-label">完成闯关</div>
      </div>
      <div class="reports-stat">
        <div class="reports-stat-value">{{ stats.averageScore }}</div>
        <div class="reports-stat-label">平均分</div>
      </div>
      <div class="reports-stat">
        <div class="reports-stat-value">{{ stats.correctRate }}%</div>
        <div class="reports-stat-label">正确率</div>
      </div>
      <div class="reports-stat">
        <div class="reports-stat-value">{{ stats.streakDays }}</div>
        <div class="reports-stat-label">连续天数</div>
      </div>
    </div>

    <!-- List -->
    <div class="reports-list" v-loading="loading">
      <div
        v-for="item in reportList"
        :key="item.sessionId"
        class="report-item"
        @click="router.push(`/report/${item.sessionId}`)"
      >
        <div :class="['report-icon', getScoreLevel(item.score)]">
          <el-icon><Document /></el-icon>
        </div>
        <div class="report-info">
          <div class="report-title">{{ item.title || '知识闯关' }}</div>
          <div class="report-meta">
            <span>{{ item.questionCount }} 题</span>
            <span>{{ formatDuration(item.durationSeconds) }}</span>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
        <div class="report-score">
          <div class="report-score-value">{{ item.score }}</div>
          <div class="report-score-label">分</div>
        </div>
        <el-icon class="report-arrow"><ArrowRight /></el-icon>
      </div>

      <el-empty v-if="!loading && reportList.length === 0" description="暂无学习报告" />
    </div>

    <!-- Pagination -->
    <div class="reports-pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchReports"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Document } from '@element-plus/icons-vue'
import { getHistory, getProfileStats, type HistoryItem, type ProfileStats } from '@/api/profile'

const router = useRouter()

const loading = ref(false)
const reportList = ref<HistoryItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref<ProfileStats>({
  totalQuizzes: 0,
  totalQuestions: 0,
  totalCorrect: 0,
  correctRate: 0,
  streakDays: 0,
  averageScore: 0
})

function getScoreLevel(score: number) {
  if (score >= 80) return 'high'
  if (score >= 60) return 'medium'
  return 'low'
}

function formatDuration(seconds: number) {
  if (!seconds) return '0秒'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return minutes > 0 ? `${minutes}分${secs}秒` : `${secs}秒`
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

async function fetchReports() {
  loading.value = true
  try {
    const data = await getHistory({
      page: currentPage.value,
      size: pageSize.value
    })
    reportList.value = data.records
    total.value = data.total
  } catch (error) {
    console.error('获取报告列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  fetchReports()
  try {
    stats.value = await getProfileStats()
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
})
</script>

<style scoped>
.reports-page {
  max-width: 960px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.page-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  background: white;
  border-bottom: 1px solid var(--border);
}

.page-nav-back {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.page-nav-back:hover {
  color: var(--primary);
}

.page-nav-title {
  font-size: 16px;
  font-weight: 700;
}

.page-nav-action {
  min-width: 40px;
}

.reports-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px 40px;
  border-bottom: 1px solid var(--border);
  background: white;
}

.reports-stat {
  background: var(--bg);
  border-radius: var(--radius-sm);
  padding: 16px;
  text-align: center;
}

.reports-stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--primary);
}

.reports-stat-label {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
}

.reports-list {
  padding: 20px 40px;
  min-height: 300px;
}

.report-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.report-item:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow);
}

.report-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.report-icon.high {
  background: var(--success-light);
  color: var(--success);
}

.report-icon.medium {
  background: #FFF8E1;
  color: #F39C12;
}

.report-icon.low {
  background: var(--danger-light);
  color: var(--danger);
}

.report-info {
  flex: 1;
}

.report-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.report-meta {
  font-size: 12px;
  color: var(--text-light);
  display: flex;
  gap: 12px;
}

.report-score {
  text-align: right;
  margin-right: 8px;
}

.report-score-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--primary);
}

.report-score-label {
  font-size: 11px;
  color: var(--text-light);
}

.report-arrow {
  color: var(--text-light);
  flex-shrink: 0;
}

.reports-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 40px 32px;
}

@media (max-width: 768px) {
  .page-nav,
  .reports-summary,
  .reports-list,
  .reports-pagination {
    padding-left: 20px;
    padding-right: 20px;
  }

  .reports-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
