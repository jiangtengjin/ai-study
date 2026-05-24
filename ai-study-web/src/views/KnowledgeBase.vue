<template>
  <div class="kb-page">
    <!-- Nav -->
    <div class="page-nav">
      <div class="page-nav-back" @click="router.push('/')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </div>
      <div class="page-nav-title">知识库管理</div>
      <div class="page-nav-action">
        <el-button type="primary" size="small" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建知识库
        </el-button>
      </div>
    </div>

    <!-- List -->
    <div class="kb-list" v-loading="loading">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        class="kb-item"
        @click="goToDetail(kb.id)"
      >
        <div class="kb-icon">
          <el-icon><Files /></el-icon>
        </div>
        <div class="kb-info">
          <div class="kb-name">{{ kb.name }}</div>
          <div class="kb-meta">
            <span v-if="kb.description">{{ kb.description }}</span>
            <span>{{ kb.docCount }} 个文档</span>
            <span>{{ formatDate(kb.createdAt) }}</span>
          </div>
        </div>
        <el-button
          type="danger"
          text
          @click.stop="handleDelete(kb)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库">
        <el-button type="primary" @click="showCreateDialog = true">创建知识库</el-button>
      </el-empty>
    </div>

    <!-- 创建知识库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建知识库"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input
            v-model="createForm.name"
            placeholder="例如：软件工程期末复习"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选，简要描述知识库用途"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Files, Delete } from '@element-plus/icons-vue'
import {
  listKnowledgeBases,
  createKnowledgeBase,
  deleteKnowledgeBase,
  type KnowledgeBaseVO,
} from '@/api/knowledgeBase'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const knowledgeBases = ref<KnowledgeBaseVO[]>([])
const showCreateDialog = ref(false)
const createForm = ref({ name: '', description: '' })

async function loadList() {
  loading.value = true
  try {
    knowledgeBases.value = await listKnowledgeBases()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    await createKnowledgeBase({
      name: createForm.value.name.trim(),
      description: createForm.value.description.trim() || undefined,
    })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', description: '' }
    await loadList()
  } catch {
    // handled by interceptor
  } finally {
    creating.value = false
  }
}

async function handleDelete(kb: KnowledgeBaseVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除知识库「${kb.name}」？删除后该知识库下的所有文档和向量数据将被清除。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteKnowledgeBase(kb.id)
    ElMessage.success('已删除')
    await loadList()
  } catch {
    // cancelled or error
  }
}

function goToDetail(id: number) {
  router.push(`/knowledge-base/${id}`)
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(loadList)
</script>

<style scoped>
.kb-page {
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

.kb-list {
  padding: 20px 40px;
  min-height: 300px;
}

.kb-item {
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

.kb-item:hover {
  border-color: var(--primary-light);
  box-shadow: var(--shadow);
}

.kb-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  background: var(--primary-light);
  color: var(--primary);
}

.kb-info {
  flex: 1;
}

.kb-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.kb-meta {
  font-size: 12px;
  color: var(--text-light);
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-nav,
  .kb-list {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
