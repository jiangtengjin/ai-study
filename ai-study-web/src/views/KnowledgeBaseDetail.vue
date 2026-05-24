<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="router.push('/knowledge-base')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>{{ detail?.name || '知识库详情' }}</h2>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      加载中...
    </div>

    <template v-else-if="detail">
      <div class="kb-meta-bar">
        <span v-if="detail.description" class="kb-desc">{{ detail.description }}</span>
        <span class="doc-count">{{ detail.docCount }} 个文档</span>
      </div>

      <!-- 文档上传区域 -->
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept=".pdf,.doc,.docx,.txt"
          drag
        >
          <el-icon :size="40"><UploadFilled /></el-icon>
          <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
          <div class="upload-tip">支持 PDF、Word、TXT 格式，单文件最大 10MB</div>
        </el-upload>
      </div>

      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在上传并处理文档，请稍候...</span>
      </div>

      <!-- 文档列表 -->
      <div class="doc-section">
        <h3>已上传文档</h3>
        <div v-if="detail.documents.length === 0" class="empty-docs">
          <p>暂无文档，请上传文档后即可基于文档内容出题</p>
        </div>
        <div v-else class="doc-list">
          <div v-for="doc in detail.documents" :key="doc.id" class="doc-item">
            <div class="doc-icon">
              <el-icon :size="24" :color="getFileColor(doc.fileType)">
                <Document />
              </el-icon>
            </div>
            <div class="doc-info">
              <span class="doc-name">{{ doc.fileName }}</span>
              <span class="doc-meta">
                {{ formatSize(doc.fileSize) }} · {{ formatDate(doc.createdAt) }}
                <el-tag v-if="doc.status === 'processing'" size="small" type="warning">处理中</el-tag>
                <el-tag v-else-if="doc.status === 'failed'" size="small" type="danger">处理失败</el-tag>
                <el-tag v-else size="small" type="success">已完成</el-tag>
              </span>
            </div>
            <el-button
              type="danger"
              text
              @click="handleDeleteDoc(doc)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import {
  getKnowledgeBaseDetail,
  uploadDocument,
  deleteDocument,
  type KnowledgeBaseDetailVO,
  type DocumentVO,
} from '@/api/knowledgeBase'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)

const loading = ref(false)
const uploading = ref(false)
const detail = ref<KnowledgeBaseDetailVO | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getKnowledgeBaseDetail(kbId)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleFileChange(file: UploadFile) {
  if (!file.raw) return

  const allowedTypes = ['application/pdf', 'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain']
  const allowedExts = ['.pdf', '.doc', '.docx', '.txt']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()

  if (!allowedExts.includes(ext)) {
    ElMessage.warning('不支持的文件格式，请上传 PDF、Word 或 TXT 文件')
    return
  }
  if (file.size && file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小超出限制，最大支持 10MB')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(kbId, file.raw)
    ElMessage.success('文档上传成功')
    await loadDetail()
  } catch {
    // handled by interceptor
  } finally {
    uploading.value = false
  }
}

async function handleDeleteDoc(doc: DocumentVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除文档「${doc.fileName}」？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteDocument(kbId, doc.id)
    ElMessage.success('已删除')
    await loadDetail()
  } catch {
    // cancelled or error
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function getFileColor(fileType: string) {
  switch (fileType) {
    case 'pdf': return '#e74c3c'
    case 'doc': case 'docx': return '#2980b9'
    case 'txt': return '#27ae60'
    default: return '#909399'
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.kb-meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: var(--radius-sm);
}

.kb-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

.doc-count {
  font-size: 13px;
  color: var(--text-tertiary);
}

.upload-section {
  margin-bottom: 20px;
}

.upload-section :deep(.el-upload-dragger) {
  padding: 32px;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.upload-text em {
  color: var(--primary);
  font-style: normal;
}

.upload-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.upload-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: var(--radius-sm);
  color: #e6a23c;
  margin-bottom: 20px;
  font-size: 14px;
}

.doc-section h3 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.empty-docs {
  text-align: center;
  padding: 40px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.doc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
}

.doc-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}
</style>
