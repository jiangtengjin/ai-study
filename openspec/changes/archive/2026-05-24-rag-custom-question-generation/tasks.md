## 1. 项目配置与依赖

- [x] 1.1 在 pom.xml 中添加 spring-ai-tika-document-reader 依赖
- [x] 1.2 在 application.yml 中新增文件上传配置（max-file-size: 10MB）和向量存储路径配置（vectorstore.store-path）
- [x] 1.3 验证现有 Spring AI 1.0.0-M6 的 VectorStore 和 TokenTextSplitter API 可用性

## 2. 数据库变更

- [x] 2.1 在 schema.sql 中新增 t_knowledge_base 表（id, user_id, name, description, doc_count, created_at, updated_at）
- [x] 2.2 在 schema.sql 中新增 t_document 表（id, knowledge_base_id, file_name, file_size, file_type, status, created_at）
- [x] 2.3 创建 KnowledgeBase 实体类和 Document 实体类
- [x] 2.4 创建 KnowledgeBaseMapper 和 DocumentMapper

## 3. 文档上传与解析服务

- [x] 3.1 创建 DocumentService，实现文档上传方法：接收 MultipartFile，校验格式和大小
- [x] 3.2 实现文档内容提取：使用 TikaDocumentReader 解析 PDF/Word/TXT 文件为纯文本
- [x] 3.3 实现文本清洗：去除多余空白和特殊字符
- [x] 3.4 实现文档分块：使用 TokenTextSplitter 将文本分割为 chunks（chunkSize=800, overlap=200）

## 4. 向量存储服务

- [x] 4.1 创建 VectorStoreConfig 配置类，配置 SimpleVectorStore Bean（带 EmbeddingModel）
- [x] 4.2 创建 KnowledgeBaseVectorStoreService，实现知识库级别的向量存储管理
- [x] 4.3 实现文档向量化入库：为每个 chunk 添加 knowledgeBaseId 和 documentId 元数据，调用 vectorStore.add()
- [x] 4.4 实现向量检索方法：通过 SearchRequest.builder() 按 knowledgeBaseId 过滤，topK=5，similarityThreshold=0.7
- [x] 4.5 实现向量数据持久化：每个知识库对应一个 JSON 文件，保存到配置的 store-path 目录
- [x] 4.6 实现文档删除时的向量清理：按 documentId 元数据删除对应向量
- [x] 4.7 实现知识库删除时的向量清理：删除整个知识库的向量文件

## 5. 知识库管理服务

- [x] 5.1 创建 KnowledgeBaseService，实现创建知识库方法（校验名称长度、创建记录）
- [x] 5.2 实现获取知识库列表方法（按用户 ID 查询，按创建时间倒序）
- [x] 5.3 实现获取知识库详情方法（包含文档列表，校验用户权限）
- [x] 5.4 实现删除知识库方法（校验用户权限，级联删除文档记录和向量数据）
- [x] 5.5 实现删除文档方法（校验文档存在性，清理向量数据，更新知识库文档数量）

## 6. 知识库管理 API

- [x] 6.1 创建 KnowledgeBaseController，实现 POST /api/v1/knowledge-base 接口（创建知识库）
- [x] 6.2 实现 GET /api/v1/knowledge-base 接口（获取知识库列表）
- [x] 6.3 实现 GET /api/v1/knowledge-base/{id} 接口（获取知识库详情）
- [x] 6.4 实现 DELETE /api/v1/knowledge-base/{id} 接口（删除知识库）
- [x] 6.5 实现 POST /api/v1/knowledge-base/{id}/documents 接口（上传文档到知识库）
- [x] 6.6 实现 DELETE /api/v1/knowledge-base/{id}/documents/{docId} 接口（删除文档）
- [x] 6.7 创建 DTO 类：CreateKnowledgeBaseRequest、UploadDocumentRequest
- [x] 6.8 创建 VO 类：KnowledgeBaseVO、KnowledgeBaseDetailVO、DocumentVO

## 7. RAG 出题集成

- [x] 7.1 修改 CreateQuizRequest DTO，新增可选字段 knowledgeBaseId
- [x] 7.2 修改 QuizService.createSession() 方法，当 knowledgeBaseId 不为空时调用向量检索
- [x] 7.3 在 AiService.generateQuestions() 方法中新增 ragResults 参数，注入出题 Prompt
- [x] 7.4 修改出题 Prompt 模板（prompts/generate-questions.txt），新增 RAG 结果注入段
- [x] 7.5 实现知识库与联网搜索的互斥逻辑：有知识库时优先使用 RAG，忽略 enableSearch

## 8. 前端 - 知识库管理页面

- [x] 8.1 创建知识库 API 封装（src/api/knowledgeBase.ts）
- [x] 8.2 创建知识库列表页面（views/KnowledgeBase.vue）
- [x] 8.3 创建创建知识库对话框组件
- [x] 8.4 创建文档上传组件（支持拖拽上传，显示上传进度和处理状态）
- [x] 8.5 创建知识库详情页面（显示文档列表，支持删除操作）
- [x] 8.6 在路由配置中添加知识库相关路由
- [x] 8.7 在导航栏中添加知识库入口

## 9. 前端 - 出题页面集成

- [x] 9.1 修改首页（Home.vue），在出题参数区域新增知识库选择下拉框
- [x] 9.2 实现知识库选择与联网搜索开关的互斥交互
- [x] 9.3 修改 createQuiz API 调用，传递 knowledgeBaseId 参数

## 10. 测试与验证

- [ ] 10.1 测试文档上传：PDF、Word、TXT 三种格式各上传一个，验证内容提取正确
- [ ] 10.2 测试知识库 CRUD：创建、查看列表、查看详情、删除知识库
- [ ] 10.3 测试 RAG 出题：从知识库出题，验证题目内容与文档相关
- [ ] 10.4 测试降级场景：知识库为空时、RAG 无结果时的出题行为
- [ ] 10.5 测试互斥逻辑：选择知识库时联网搜索应被忽略
