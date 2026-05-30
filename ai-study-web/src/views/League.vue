<template>
  <div class="league-page">
    <!-- Nav -->
    <div class="page-nav">
      <div class="page-nav-back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">段位排行</div>
      <div class="page-nav-action"></div>
    </div>

    <!-- Tier Header -->
    <div class="league-header">
      <div class="tier-display">
        <div class="tier-icon" v-html="tierInfo?.tierIcon || '🥉'"></div>
        <div class="tier-info">
          <div class="tier-name">{{ tierInfo?.tierName || '铜牌' }}段位</div>
          <div class="tier-points">累计积分：{{ tierInfo?.totalPoints?.toLocaleString() || 0 }}</div>
        </div>
      </div>
      <div class="tier-progress" v-if="tierInfo?.nextTierName">
        <div class="progress-text">
          距离 <span class="next-tier"><span v-html="tierInfo.nextTierIcon"></span> {{ tierInfo.nextTierName }}</span>
          还需 <span class="points-need">{{ tierInfo.pointsToNext?.toLocaleString() }}</span> 积分
        </div>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="10"
          :show-text="false"
          color="var(--primary)"
        />
      </div>
      <div class="tier-max" v-else>
        <span>已达到最高段位</span>
      </div>
    </div>

    <!-- Tabs -->
    <div class="league-tabs">
      <div
        :class="['tab-item', { active: activeTab === 'ranking' }]"
        @click="activeTab = 'ranking'"
      >
        本周排名
      </div>
      <div
        :class="['tab-item', { active: activeTab === 'history' }]"
        @click="activeTab = 'history'"
      >
        联赛历史
      </div>
    </div>

    <!-- Ranking Tab -->
    <div v-show="activeTab === 'ranking'" class="tab-content" v-loading="activeTab === 'ranking' && rankingLoading">
      <!-- My Ranking Card -->
      <div class="my-ranking-card" v-if="rankingData.myRanking > 0">
        <div class="my-rank-badge" :class="rankClass(rankingData.myRanking)">
          {{ rankingData.myRanking }}
        </div>
        <div class="my-rank-info">
          <div class="my-rank-position">我的排名</div>
          <div class="my-rank-points">本周积分：{{ rankingData.myPoints }}</div>
        </div>
        <div class="my-rank-promote" v-if="rankingData.pointsToPromote > 0">
          距晋级线还差 <strong>{{ rankingData.pointsToPromote }}</strong> 积分
        </div>
        <div class="my-rank-promote safe" v-else-if="rankingData.myRanking > 0">
          已在晋级区内
        </div>
      </div>

      <!-- Quota Info -->
      <div class="quota-info" v-if="rankingData.rankings?.length > 0">
        <span class="quota-item promote">晋级 Top {{ rankingData.promoteCount }}</span>
        <span class="quota-item demote" v-if="rankingData.demoteCount > 0">降级 Bottom {{ rankingData.demoteCount }}</span>
      </div>

      <!-- Ranking List -->
      <div class="ranking-list" v-if="rankingData.rankings?.length > 0">
        <div
          v-for="item in rankingData.rankings"
          :key="item.userId"
          :class="['ranking-item', { 'is-me': item.ranking === rankingData.myRanking }]"
        >
          <div :class="['rank-number', rankClass(item.ranking)]">
            {{ item.ranking }}
          </div>
          <div class="rank-avatar">
            {{ item.nickname?.charAt(0) || '?' }}
          </div>
          <div class="rank-name">{{ item.nickname }}</div>
          <div class="rank-points">{{ item.weeklyPoints }} 分</div>
          <div class="rank-zone">
            <span v-if="item.ranking <= (rankingData.promoteCount || 0)" class="zone-promote">晋级</span>
            <span v-else-if="rankingData.demoteCount > 0 && item.ranking > (rankingData.rankings?.length || 0) - (rankingData.demoteCount || 0)" class="zone-demote">降级</span>
            <span v-else class="zone-keep">保级</span>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <el-empty v-if="!rankingLoading && (!rankingData.rankings || rankingData.rankings.length === 0)" description="本周暂未分组，下周一自动匹配" />
    </div>

    <!-- History Tab -->
    <div v-show="activeTab === 'history'" class="tab-content" v-loading="activeTab === 'history' && historyLoading">
      <div class="history-list" v-if="historyList.length > 0">
        <div v-for="item in historyList" :key="item.weekStartDate" class="history-item">
          <div class="history-week">
            <div class="week-date">{{ formatWeekDate(item.weekStartDate) }}</div>
            <div class="week-tier"><span v-html="item.tierIcon"></span> {{ item.tierName }}</div>
          </div>
          <div class="history-detail">
            <div class="detail-rank">第 {{ item.ranking }} 名</div>
            <div class="detail-points">{{ item.weeklyPoints }} 积分</div>
          </div>
          <div :class="['history-result', item.result]">
            {{ resultText(item.result) }}
          </div>
        </div>
      </div>

      <el-empty v-if="!historyLoading && historyList.length === 0" description="暂无联赛历史记录" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getLeagueTier, getLeagueRanking, getLeagueHistory } from '@/api/league'
import type { LeagueTierInfo, LeagueRanking, LeagueHistoryItem } from '@/api/league'

const router = useRouter()

const activeTab = ref<'ranking' | 'history'>('ranking')
const tierInfo = ref<LeagueTierInfo | null>(null)
const rankingData = ref<LeagueRanking>({
  groupId: 0,
  tierName: '',
  tierIcon: '',
  rankings: [],
  myRanking: 0,
  myPoints: 0,
  pointsToPromote: 0,
  promoteCount: 0,
  demoteCount: 0
})
const historyList = ref<LeagueHistoryItem[]>([])
const rankingLoading = ref(false)
const historyLoading = ref(false)

const progressPercent = computed(() => {
  if (!tierInfo.value?.nextTierPoints || !tierInfo.value?.totalPoints) return 0
  const current = tierInfo.value.totalPoints
  const target = tierInfo.value.nextTierPoints
  const prevThreshold = (tierInfo.value.tierSortOrder > 1) ? current - (tierInfo.value.pointsToNext || 0) : 0
  const range = target - prevThreshold
  if (range <= 0) return 100
  return Math.min(100, Math.round(((current - prevThreshold) / range) * 100))
})

function rankClass(rank: number): string {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return 'bronze'
  return ''
}

function formatWeekDate(dateStr: string): string {
  if (!dateStr) return ''
  const start = new Date(dateStr)
  const end = new Date(start)
  end.setDate(end.getDate() + 6)
  const fmt = (d: Date) => `${d.getMonth() + 1}/${d.getDate()}`
  return `${fmt(start)} - ${fmt(end)}`
}

function resultText(result: string): string {
  switch (result) {
    case 'promote': return '晋级'
    case 'demote': return '降级'
    case 'keep': return '保级'
    default: return result || '-'
  }
}

async function fetchTier() {
  try {
    tierInfo.value = await getLeagueTier()
  } catch (error) {
    console.error('获取段位信息失败:', error)
  }
}

async function fetchRanking() {
  rankingLoading.value = true
  try {
    rankingData.value = await getLeagueRanking()
  } catch (error) {
    console.error('获取排名信息失败:', error)
  } finally {
    rankingLoading.value = false
  }
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    historyList.value = await getLeagueHistory(12)
  } catch (error) {
    console.error('获取联赛历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'ranking' && !rankingData.value.rankings?.length) {
    fetchRanking()
  } else if (tab === 'history' && historyList.value.length === 0) {
    fetchHistory()
  }
})

onMounted(() => {
  fetchTier()
  fetchRanking()
})
</script>

<style scoped>
.league-page {
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

/* Tier Header */
.league-header {
  background: linear-gradient(135deg, var(--primary), #8B7CF7);
  padding: 32px 40px;
  color: white;
}

.tier-display {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.tier-icon {
  font-size: 48px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.tier-info {
  flex: 1;
}

.tier-name {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 4px;
}

.tier-points {
  font-size: 14px;
  opacity: 0.85;
}

.tier-progress {
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  padding: 16px;
}

.progress-text {
  font-size: 13px;
  margin-bottom: 10px;
  opacity: 0.9;
}

.next-tier {
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.next-tier :deep(svg) {
  width: 32px;
  height: 32px;
}

.points-need {
  font-weight: 800;
  font-size: 16px;
}

.tier-progress :deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.2);
}

.tier-max {
  font-size: 14px;
  opacity: 0.85;
  text-align: center;
  padding: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
}

/* Tabs */
.league-tabs {
  display: flex;
  background: white;
  border-bottom: 1px solid var(--border);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: var(--primary);
}

.tab-item.active {
  color: var(--primary);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 30%;
  right: 30%;
  height: 3px;
  background: var(--primary);
  border-radius: 2px;
}

/* Tab Content */
.tab-content {
  padding: 20px 40px 32px;
  min-height: 300px;
}

/* My Ranking Card */
.my-ranking-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--primary-bg);
  border: 1px solid rgba(108, 92, 231, 0.15);
  border-radius: var(--radius);
  margin-bottom: 16px;
}

.my-rank-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 800;
  flex-shrink: 0;
}

.my-rank-badge.gold {
  background: linear-gradient(135deg, #F39C12, #F1C40F);
}

.my-rank-badge.silver {
  background: linear-gradient(135deg, #95A5A6, #BDC3C7);
}

.my-rank-badge.bronze {
  background: linear-gradient(135deg, #E67E22, #D35400);
}

.my-rank-info {
  flex: 1;
}

.my-rank-position {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.my-rank-points {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.my-rank-promote {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: right;
}

.my-rank-promote strong {
  color: var(--primary);
  font-size: 16px;
}

.my-rank-promote.safe {
  color: var(--success);
  font-weight: 600;
}

/* Quota Info */
.quota-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.quota-item {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
}

.quota-item.promote {
  background: var(--success-light);
  color: var(--success);
}

.quota-item.demote {
  background: var(--danger-light);
  color: var(--danger);
}

/* Ranking List */
.ranking-list {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid var(--border);
  transition: background 0.2s;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-item:hover {
  background: var(--bg);
}

.ranking-item.is-me {
  background: var(--primary-bg);
}

.rank-number {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.rank-number.gold {
  background: linear-gradient(135deg, #F39C12, #F1C40F);
  color: white;
}

.rank-number.silver {
  background: linear-gradient(135deg, #95A5A6, #BDC3C7);
  color: white;
}

.rank-number.bronze {
  background: linear-gradient(135deg, #E67E22, #D35400);
  color: white;
}

.rank-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-light), var(--primary));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.rank-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.rank-points {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary);
  min-width: 60px;
  text-align: right;
}

.rank-zone {
  min-width: 40px;
  text-align: center;
}

.rank-zone span {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
}

.zone-promote {
  background: var(--success-light);
  color: var(--success);
}

.zone-demote {
  background: var(--danger-light);
  color: var(--danger);
}

.zone-keep {
  background: var(--bg);
  color: var(--text-secondary);
}

/* History */
.history-list {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: white;
  border-bottom: 1px solid var(--border);
}

.history-item:last-child {
  border-bottom: none;
}

.history-week {
  flex: 1;
}

.week-date {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.week-tier {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.history-detail {
  text-align: center;
  min-width: 80px;
}

.detail-rank {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.detail-points {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.history-result {
  min-width: 52px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 8px;
}

.history-result.promote {
  background: var(--success-light);
  color: var(--success);
}

.history-result.demote {
  background: var(--danger-light);
  color: var(--danger);
}

.history-result.keep {
  background: var(--bg);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .page-nav,
  .tab-content {
    padding-left: 20px;
    padding-right: 20px;
  }

  .league-header {
    padding: 24px 20px;
  }

  .tier-icon {
    width: 60px;
    height: 60px;
    font-size: 36px;
  }

  .tier-name {
    font-size: 20px;
  }

  .my-ranking-card {
    flex-wrap: wrap;
  }

  .my-rank-promote {
    width: 100%;
    text-align: left;
    margin-top: 4px;
  }
}
</style>
