import request from './request'

export interface KnowledgeBaseVO {
  id: number
  name: string
  description: string | null
  docCount: number
  createdAt: string
}

export interface DocumentVO {
  id: number
  fileName: string
  fileSize: number
  fileType: string
  status: string
  createdAt: string
}

export interface KnowledgeBaseDetailVO {
  id: number
  name: string
  description: string | null
  docCount: number
  createdAt: string
  documents: DocumentVO[]
}

export interface CreateKnowledgeBaseParams {
  name: string
  description?: string
}

// 创建知识库
export function createKnowledgeBase(data: CreateKnowledgeBaseParams) {
  return request.post<KnowledgeBaseVO>('/v1/knowledge-base', data)
}

// 获取知识库列表
export function listKnowledgeBases() {
  return request.get<KnowledgeBaseVO[]>('/v1/knowledge-base')
}

// 获取知识库详情
export function getKnowledgeBaseDetail(id: number) {
  return request.get<KnowledgeBaseDetailVO>(`/v1/knowledge-base/${id}`)
}

// 删除知识库
export function deleteKnowledgeBase(id: number) {
  return request.delete<void>(`/v1/knowledge-base/${id}`)
}

// 上传文档到知识库
export function uploadDocument(kbId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<DocumentVO>(`/v1/knowledge-base/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 文档处理可能较慢
  })
}

// 删除文档
export function deleteDocument(kbId: number, docId: number) {
  return request.delete<void>(`/v1/knowledge-base/${kbId}/documents/${docId}`)
}
