<template>
  <el-card class="duration-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <el-icon><Timer /></el-icon>
        <span>学习时长统计</span>
      </div>
    </template>
    <div class="stats-grid">
      <div class="stat-item">
        <div class="stat-value">{{ totalHours }}</div>
        <div class="stat-label">总学习时长（小时）</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ avgMinutes }}</div>
        <div class="stat-label">日均学习时长（分钟）</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Timer } from '@element-plus/icons-vue'
import type { TrendStats } from '@/api/trend'

const props = defineProps<{
  data: TrendStats[]
}>()

const totalMinutes = computed(() => {
  return props.data.reduce((sum, item) => sum + item.studyDuration, 0)
})

const totalHours = computed(() => {
  return (totalMinutes.value / 60).toFixed(1)
})

const avgMinutes = computed(() => {
  if (props.data.length === 0) return 0
  return Math.round(totalMinutes.value / props.data.length)
})
</script>

<style scoped>
.duration-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  text-align: center;
}

.stat-item {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
