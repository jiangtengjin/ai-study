<template>
  <div class="history-page">
    <!-- Nav -->
    <div class="page-nav">
      <div class="page-nav-back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">历史记录</div>
      <div class="page-nav-action"></div>
    </div>

    <!-- Filters -->
    <div class="history-filters">
      <div
        v-for="filter in filters"
        :key="filter.value"
        :class="['filter-chip', { active: currentFilter === filter.value }]"
        @click="handleFilterChange(filter.value)"
      >
        {{ filter.label }}
      </div>
    </div>

    <!-- List -->
    <div class="history-list" v-loading="loading">
      <div
        v-for="item in historyList"
        :key="item.sessionId"
        class="history-item"
        @click="router.push(`/report/${item.sessionId}`)"
      >
        <div :class="['history-icon', getScoreLevel(item.score)]">
          <el-icon><Document /></el-icon>
        </div>
        <div class="history-info">
          <div class="history-title">{{ item.title || '知识闯关' }}</div>
          <div class="history-meta">
            <span>{{ item.questionCount }} 题</span>
            <span>{{ formatDuration(item.durationSeconds) }}</span>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
        <div class="history-score">
          <div class="history-score-value">{{ item.score }}</div>
          <div class="history-score-label">分</div>
        </div>
      </div>

      <el-empty v-if="!loading && historyList.length === 0" description="暂无历史记录" />
    </div>

    <!-- Pagination -->
    <div class="history-pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchHistory"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Document } from '@element-plus/icons-vue'
import { getHistory, type HistoryItem } from '@/api/profile'

const router = useRouter()

const loading = ref(false)
const historyList = ref<HistoryItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentFilter = ref('')

const filters = [
  { label: '全部', value: '' },
  { label: '高分 (≥80)', value: 'high' },
  { label: '低分 (<60)', value: 'low' }
]

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

async function fetchHistory() {
  loading.value = true
  try {
    const data = await getHistory({
      page: currentPage.value,
      size: pageSize.value,
      filter: currentFilter.value || undefined
    })
    historyList.value = data.records
    total.value = data.total
  } catch (error) {
    console.error('获取历史记录失败:', error)
  } finally {
    loading.value = false
  }
}

function handleFilterChange(filter: string) {
  currentFilter.value = filter
  currentPage.value = 1
  fetchHistory()
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.history-page {
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
  font-size: 13px;
  color: var(--primary);
  font-weight: 600;
  min-width: 40px;
}

.history-filters {
  display: flex;
  gap: 12px;
  padding: 20px 40px;
  border-bottom: 1px solid var(--border);
  background: white;
}

.filter-chip {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: white;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.filter-chip.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.filter-chip:hover:not(.active) {
  border-color: var(--primary-light);
}

.history-list {
  padding: 20px 40px;
  min-height: 300px;
}

.history-item {
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

.history-item:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow);
}

.history-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.history-icon.high {
  background: var(--success-light);
  color: var(--success);
}

.history-icon.medium {
  background: #FFF8E1;
  color: #F39C12;
}

.history-icon.low {
  background: var(--danger-light);
  color: var(--danger);
}

.history-info {
  flex: 1;
}

.history-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.history-meta {
  font-size: 12px;
  color: var(--text-light);
  display: flex;
  gap: 12px;
}

.history-score {
  text-align: right;
}

.history-score-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--primary);
}

.history-score-label {
  font-size: 11px;
  color: var(--text-light);
}

.history-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 40px 32px;
}

@media (max-width: 768px) {
  .page-nav,
  .history-filters,
  .history-list,
  .history-pagination {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
