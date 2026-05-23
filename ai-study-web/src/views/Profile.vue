<template>
  <div class="profile-page">
    <!-- Profile Header -->
    <div class="profile-header">
      <div class="profile-info">
        <div class="profile-avatar">
          {{ userStore.nickname?.charAt(0) || '?' }}
        </div>
        <div>
          <div class="profile-name">{{ userStore.nickname }}</div>
          <div class="profile-meta">
            加入 {{ joinDays }} 天 · 已完成 {{ stats.totalQuizzes }} 次闯关
          </div>
        </div>
      </div>
      <div class="profile-stats-row">
        <div class="profile-stat">
          <div class="profile-stat-value">{{ stats.totalQuestions }}</div>
          <div class="profile-stat-label">总答题数</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-value">{{ stats.correctRate }}%</div>
          <div class="profile-stat-label">正确率</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-value">{{ stats.streakDays }}</div>
          <div class="profile-stat-label">连续天数</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-value">{{ stats.averageScore }}</div>
          <div class="profile-stat-label">平均分</div>
        </div>
      </div>
    </div>

    <!-- Profile Body -->
    <div class="profile-body">
      <!-- Streak -->
      <div class="profile-section">
        <div class="profile-section-title">
          <el-icon><Sunny /></el-icon> 连续学习
        </div>
        <div class="streak-card">
          <div class="streak-icon">
            <el-icon :size="40" color="#F39C12"><Sunny /></el-icon>
          </div>
          <div class="streak-info">
            <div class="streak-count">{{ stats.streakDays }} 天</div>
            <div class="streak-label">继续保持，距离下一个成就还差 {{ nextAchievementDays }} 天！</div>
          </div>
          <div class="streak-days">
            <div
              v-for="(day, index) in weekDays"
              :key="index"
              :class="['streak-day', day.status]"
            >
              {{ day.label }}
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="profile-section">
        <div class="profile-section-title">
          <el-icon><Lightning /></el-icon> 快捷入口
        </div>
        <div class="quick-actions">
          <div class="quick-action" @click="router.push('/profile/history')">
            <el-icon :size="28"><Clock /></el-icon>
            <div class="quick-action-text">历史记录</div>
          </div>
          <div class="quick-action" @click="router.push('/profile/wrong-book')">
            <el-icon :size="28"><CircleClose /></el-icon>
            <div class="quick-action-text">错题本</div>
          </div>
          <div class="quick-action" @click="router.push('/profile/reports')">
            <el-icon :size="28"><Document /></el-icon>
            <div class="quick-action-text">学习报告</div>
          </div>
          <div class="quick-action" @click="router.push('/profile/settings')">
            <el-icon :size="28"><Setting /></el-icon>
            <div class="quick-action-text">账号设置</div>
          </div>
        </div>
      </div>

      <!-- Chart -->
      <div class="profile-section">
        <div class="profile-section-title">
          <el-icon><TrendCharts /></el-icon> 学习趋势
          <el-radio-group v-model="selectedDays" @change="fetchTrendData" size="small" style="margin-left: auto;">
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
            <el-radio-button :value="90">90天</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="trendLoading" class="chart-placeholder">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <span style="margin-left: 8px; color: var(--text-secondary);">加载中...</span>
        </div>

        <template v-else-if="trendData.length > 0">
          <StudyDurationCard :data="trendData" />
          <el-row :gutter="16">
            <el-col :xs="24" :sm="24" :md="12">
              <el-card shadow="hover" style="margin-bottom: 16px;">
                <template #header>
                  <div class="chart-header">正确率趋势</div>
                </template>
                <AccuracyChart :data="trendData" />
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="24" :md="12">
              <el-card shadow="hover" style="margin-bottom: 16px;">
                <template #header>
                  <div class="chart-header">答题数量趋势</div>
                </template>
                <QuestionCountChart :data="trendData" />
              </el-card>
            </el-col>
          </el-row>
        </template>

        <div v-else class="chart-placeholder">
          <el-empty description="暂无学习数据，快去答题吧！" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sunny, Lightning, Clock, CircleClose, Document, Setting, TrendCharts, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getProfileStats, type ProfileStats } from '@/api/profile'
import { getTrendStats, type TrendStats } from '@/api/trend'
import AccuracyChart from '@/components/charts/AccuracyChart.vue'
import QuestionCountChart from '@/components/charts/QuestionCountChart.vue'
import StudyDurationCard from '@/components/charts/StudyDurationCard.vue'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<ProfileStats>({
  totalQuizzes: 0,
  totalQuestions: 0,
  totalCorrect: 0,
  correctRate: 0,
  streakDays: 0,
  averageScore: 0
})

const trendData = ref<TrendStats[]>([])
const trendLoading = ref(false)
const selectedDays = ref<7 | 30 | 90>(7)

const joinDays = computed(() => {
  if (!userStore.userInfo?.createdAt) return 0
  const created = new Date(userStore.userInfo.createdAt)
  const now = new Date()
  return Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))
})

const nextAchievementDays = computed(() => {
  const streak = stats.value.streakDays
  if (streak < 7) return 7 - streak
  if (streak < 30) return 30 - streak
  return 0
})

const weekDays = computed(() => {
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  const today = new Date().getDay()
  const streak = stats.value.streakDays

  return labels.map((label, index) => {
    const dayIndex = (index + 1) % 7 // 周一为0
    let status = 'future'
    if (dayIndex < today) {
      status = streak > 0 ? 'done' : 'future'
    } else if (dayIndex === today) {
      status = streak > 0 ? 'done' : 'today'
    }
    return { label, status }
  })
})

const fetchTrendData = async () => {
  trendLoading.value = true
  try {
    const data = await getTrendStats({
      period: 'day',
      days: selectedDays.value
    })
    console.log('趋势数据:', data)
    trendData.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取趋势数据失败:', error)
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

onMounted(async () => {
  try {
    stats.value = await getProfileStats()
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
  fetchTrendData()
})
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.profile-header {
  background: linear-gradient(135deg, var(--primary), #8B7CF7);
  padding: 40px;
  color: white;
  position: relative;
  overflow: hidden;
}

.profile-header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.profile-info {
  display: flex;
  align-items: center;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.profile-name {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.profile-meta {
  font-size: 13px;
  opacity: 0.85;
}

.profile-stats-row {
  display: flex;
  gap: 32px;
  margin-top: 32px;
  position: relative;
  z-index: 1;
}

.profile-stat {
  text-align: center;
}

.profile-stat-value {
  font-size: 28px;
  font-weight: 800;
}

.profile-stat-label {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}

.profile-body {
  padding: 32px 40px;
}

.profile-section {
  margin-bottom: 32px;
}

.profile-section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.streak-card {
  background: linear-gradient(135deg, #FFF8E1, #FFF3CD);
  border: 1px solid rgba(254, 202, 87, 0.3);
  border-radius: var(--radius);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.streak-icon {
  font-size: 40px;
}

.streak-info {
  flex: 1;
}

.streak-count {
  font-size: 24px;
  font-weight: 800;
  color: #F39C12;
}

.streak-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.streak-days {
  display: flex;
  gap: 6px;
}

.streak-day {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.streak-day.done {
  background: #F39C12;
  color: white;
}

.streak-day.today {
  background: white;
  border: 2px solid #F39C12;
  color: #F39C12;
}

.streak-day.future {
  background: white;
  border: 1px solid var(--border);
  color: var(--text-light);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-action {
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-action:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow);
}

.quick-action .el-icon {
  margin-bottom: 8px;
  color: var(--primary);
}

.quick-action-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.chart-placeholder {
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 24px;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .profile-header {
    padding: 32px 24px;
  }

  .profile-body {
    padding: 24px;
  }

  .profile-stats-row {
    gap: 20px;
  }

  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }

  .streak-days {
    display: none;
  }
}
</style>
