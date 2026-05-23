<template>
  <div class="wrongbook-page">
    <!-- Nav -->
    <div class="page-nav">
      <div class="page-nav-back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">错题本</div>
      <div class="page-nav-action"></div>
    </div>

    <!-- Summary -->
    <div class="wrongbook-summary">
      <div class="wrongbook-stat">
        <div class="wrongbook-stat-value">{{ total }}</div>
        <div class="wrongbook-stat-label">总错题数</div>
      </div>
      <div class="wrongbook-stat">
        <div class="wrongbook-stat-value">{{ knowledgePoints }}</div>
        <div class="wrongbook-stat-label">薄弱知识点</div>
      </div>
    </div>

    <!-- List -->
    <div class="wrongbook-list" v-loading="loading">
      <div
        v-for="item in wrongList"
        :key="item.answerId"
        class="wrongbook-item"
      >
        <div class="wrongbook-item-header">
          <span class="wrongbook-item-source">{{ item.sessionTitle || '知识闯关' }}</span>
          <span class="wrongbook-item-date">{{ formatDate(item.createdAt) }}</span>
        </div>
        <div class="wrongbook-question">
          {{ item.questionContent }}
        </div>
        <div class="wrongbook-options">
          <div
            :class="['wrongbook-option', getOptionClass('A', item)]"
          >
            <div class="wrongbook-option-letter">A</div>
            <span>{{ item.optionA }}</span>
          </div>
          <div
            :class="['wrongbook-option', getOptionClass('B', item)]"
          >
            <div class="wrongbook-option-letter">B</div>
            <span>{{ item.optionB }}</span>
          </div>
          <div
            :class="['wrongbook-option', getOptionClass('C', item)]"
          >
            <div class="wrongbook-option-letter">C</div>
            <span>{{ item.optionC }}</span>
          </div>
          <div
            :class="['wrongbook-option', getOptionClass('D', item)]"
          >
            <div class="wrongbook-option-letter">D</div>
            <span>{{ item.optionD }}</span>
          </div>
        </div>
        <div class="wrongbook-explanation">
          <strong>解析：</strong>{{ item.explanation }}
        </div>
        <div class="wrongbook-actions">
          <el-button size="small" @click="handleRemove(item.answerId)">移除</el-button>
          <el-button type="primary" size="small" @click="handleRetry(item.sessionId)">
            重新练习
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && wrongList.length === 0" description="暂无错题" />
    </div>

    <!-- Pagination -->
    <div class="wrongbook-pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchWrongQuestions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWrongQuestions, removeWrongQuestion, type WrongQuestion } from '@/api/profile'
import { retrySession } from '@/api/quiz'

const router = useRouter()

const loading = ref(false)
const wrongList = ref<WrongQuestion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const knowledgePoints = computed(() => {
  const points = new Set(wrongList.value.map(item => item.knowledgePoint).filter(Boolean))
  return points.size
})

function getOptionClass(option: string, item: WrongQuestion) {
  if (option === item.correctAnswer) return 'correct'
  if (option === item.userAnswer) return 'wrong'
  return 'neutral'
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

async function fetchWrongQuestions() {
  loading.value = true
  try {
    const data = await getWrongQuestions({
      page: currentPage.value,
      size: pageSize.value
    })
    wrongList.value = data.records
    total.value = data.total
  } catch (error) {
    console.error('获取错题本失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleRemove(answerId: number) {
  try {
    await ElMessageBox.confirm('确定要移除这道错题吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await removeWrongQuestion(answerId)
    ElMessage.success('已移除')
    fetchWrongQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除错题失败:', error)
    }
  }
}

async function handleRetry(sessionId: number) {
  try {
    const res = await retrySession(sessionId)
    router.push(`/quiz/${res.sessionId}`)
  } catch (error) {
    console.error('创建重新练习会话失败:', error)
  }
}

onMounted(() => {
  fetchWrongQuestions()
})
</script>

<style scoped>
.wrongbook-page {
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

.wrongbook-summary {
  display: flex;
  gap: 16px;
  padding: 20px 40px;
  border-bottom: 1px solid var(--border);
  background: white;
}

.wrongbook-stat {
  flex: 1;
  background: var(--bg);
  border-radius: var(--radius-sm);
  padding: 16px;
  text-align: center;
}

.wrongbook-stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--danger);
}

.wrongbook-stat-label {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
}

.wrongbook-list {
  padding: 20px 40px;
  min-height: 300px;
}

.wrongbook-item {
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px;
  margin-bottom: 16px;
}

.wrongbook-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.wrongbook-item-source {
  font-size: 12px;
  color: var(--primary);
  font-weight: 600;
  padding: 4px 10px;
  background: rgba(108, 92, 231, 0.08);
  border-radius: 12px;
}

.wrongbook-item-date {
  font-size: 12px;
  color: var(--text-light);
}

.wrongbook-question {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  margin-bottom: 12px;
}

.wrongbook-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.wrongbook-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.wrongbook-option.correct {
  background: var(--success-light);
  color: var(--success);
  font-weight: 600;
}

.wrongbook-option.wrong {
  background: var(--danger-light);
  color: var(--danger);
  text-decoration: line-through;
  opacity: 0.7;
}

.wrongbook-option.neutral {
  background: var(--bg);
  color: var(--text-secondary);
}

.wrongbook-option-letter {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.wrongbook-option.correct .wrongbook-option-letter {
  background: var(--success);
  color: white;
}

.wrongbook-option.wrong .wrongbook-option-letter {
  background: var(--danger);
  color: white;
}

.wrongbook-option.neutral .wrongbook-option-letter {
  background: var(--border);
  color: var(--text-secondary);
}

.wrongbook-explanation {
  background: var(--bg);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.wrongbook-explanation strong {
  color: var(--text-primary);
}

.wrongbook-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.wrongbook-pagination {
  display: flex;
  justify-content: center;
  padding: 20px 40px 32px;
}

@media (max-width: 768px) {
  .page-nav,
  .wrongbook-summary,
  .wrongbook-list,
  .wrongbook-pagination {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
