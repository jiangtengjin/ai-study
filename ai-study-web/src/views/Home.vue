<template>
  <div class="page-container">
    <!-- Hero 区域 -->
    <div class="hero">
      <div class="hero-badge">AI 驱动的智能学习</div>
      <h1>学一遍不如闯一关</h1>
      <p>输入任意知识内容，AI 帮你出题闯关，高效掌握每一个知识点</p>
    </div>

    <!-- 知识输入区 -->
    <div class="input-section">
      <div class="input-card" :class="{ focused: inputFocused }">
        <el-input
          v-model="content"
          type="textarea"
          :rows="5"
          placeholder="请粘贴你想学习的知识内容...&#10;&#10;例如：机器学习中的过拟合是什么？如何避免？"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
          resize="none"
        />
        <div class="input-actions">
          <el-button text size="small" disabled>
            <el-icon><Paperclip /></el-icon>
            上传文档
          </el-button>
          <el-button text size="small" disabled>
            <el-icon><Link /></el-icon>
            网页链接
          </el-button>
          <div class="search-toggle">
            <el-icon><Search /></el-icon>
            <span>联网搜索</span>
            <el-switch v-model="enableSearch" size="small" />
          </div>
        </div>
      </div>
    </div>

    <!-- 配置区 -->
    <div class="config-section">
      <div class="config-group">
        <span class="config-label">题目数量</span>
        <div class="config-options">
          <div
            v-for="opt in countOptions"
            :key="opt"
            class="config-option"
            :class="{ active: questionCount === opt }"
            @click="questionCount = opt"
          >
            {{ opt }} 题
          </div>
        </div>
      </div>
      <div class="config-group">
        <span class="config-label">难度等级</span>
        <div class="config-options">
          <div
            v-for="opt in difficultyOptions"
            :key="opt.value"
            class="config-option"
            :class="{ active: difficulty === opt.value }"
            @click="difficulty = opt.value"
          >
            {{ opt.label }}
          </div>
        </div>
      </div>
    </div>

    <!-- 开始闯关按钮 -->
    <div class="start-section">
      <el-button
        type="primary"
        size="large"
        class="btn-start"
        :loading="loading"
        @click="handleStart"
      >
        <el-icon v-if="!loading"><VideoPlay /></el-icon>
        {{ loading ? 'AI 正在生成题目...' : '开始闯关' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createQuiz } from '@/api/quiz'

const router = useRouter()
const content = ref('')
const questionCount = ref(10)
const difficulty = ref('balanced')
const loading = ref(false)
const inputFocused = ref(false)
const enableSearch = ref(localStorage.getItem('enableSearch') === 'true')

watch(enableSearch, (val) => localStorage.setItem('enableSearch', String(val)))

const countOptions = [5, 10, 15, 20]
const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '均衡', value: 'balanced' },
  { label: '困难', value: 'hard' },
]

async function handleStart() {
  if (!content.value.trim()) {
    ElMessage.warning('请输入知识内容')
    return
  }
  if (content.value.trim().length < 10) {
    ElMessage.warning('知识内容至少 10 个字')
    return
  }

  loading.value = true
  try {
    const res = await createQuiz({
      content: content.value.trim(),
      questionCount: questionCount.value,
      difficulty: difficulty.value,
      enableSearch: enableSearch.value,
    })
    router.push(`/quiz/${res.sessionId}`)
  } catch (e) {
    // 错误已在 interceptor 中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 60px 40px 40px;
}

.hero-badge {
  display: inline-block;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.1), rgba(0, 210, 211, 0.1));
  color: var(--primary);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
}

.hero h1 {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary), var(--success));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.hero p {
  font-size: 16px;
  color: var(--text-secondary);
}

.input-section {
  padding: 0 40px 40px;
  max-width: 720px;
  margin: 0 auto;
}

.input-card {
  background: white;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  padding: 24px;
  transition: border-color 0.3s;
}

.input-card.focused {
  border-color: var(--primary-light);
}

.input-card :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  background: transparent;
}

.input-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.search-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.config-section {
  padding: 0 40px 32px;
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  gap: 40px;
  justify-content: center;
  flex-wrap: nowrap;
}

.config-group {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.config-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.config-options {
  display: flex;
  gap: 8px;
}

.config-option {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: white;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.config-option.active {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.config-option:hover:not(.active) {
  border-color: var(--primary-light);
  color: var(--primary);
}

.start-section {
  text-align: center;
  padding: 0 40px 48px;
}

.btn-start {
  padding: 16px 48px !important;
  font-size: 17px !important;
  font-weight: 700 !important;
  border-radius: var(--radius) !important;
  background: linear-gradient(135deg, var(--primary), var(--primary-light)) !important;
  border: none !important;
  box-shadow: 0 8px 24px rgba(108, 92, 231, 0.3) !important;
}

.btn-start:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(108, 92, 231, 0.4) !important;
}

@media (max-width: 768px) {
  .hero { padding: 40px 20px 24px; }
  .hero h1 { font-size: 28px; }
  .input-section, .config-section, .start-section {
    padding-left: 20px;
    padding-right: 20px;
  }
  .config-section {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
