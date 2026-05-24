## Why

当前系统仅支持用户手动输入一段文字或 URL 来生成题目，无法针对特定学科或考试进行定制化出题。例如软件工程专业的学生临近期末考试，需要根据课程知识点突击复习，但通过当前方式向 AI 提问得到的题目必然是通用、泛化的，缺乏专业性和针对性。需要引入 RAG（检索增强生成）能力，让用户上传自己的知识文档（课件、笔记、教材），系统基于文档内容生成高度针对性的题目。

## What Changes

- 新增文档上传接口，支持 PDF、Word、TXT 格式文件上传和内容提取
- 新增知识库管理功能，用户可以创建、查看、删除自己的知识库
- 引入向量存储（Spring AI SimpleVectorStore），将文档内容分块并向量化存储
- 新增基于知识库出题的流程：用户选择知识库 → 检索相关文档片段 → 注入出题 Prompt → 生成针对性题目
- 修改出题 API，增加 `knowledgeBaseId` 参数支持从知识库出题
- 前端新增知识库管理页面和出题时的知识库选择交互

## Capabilities

### New Capabilities
- `document-upload`: 文档上传与内容提取，支持 PDF/Word/TXT 格式文件解析为纯文本
- `knowledge-base`: 知识库管理，包括创建知识库、上传文档到知识库、查看/删除知识库
- `rag-question-generation`: 基于 RAG 的出题，从知识库中检索相关文档片段，注入出题 Prompt 生成针对性题目

### Modified Capabilities
- `question-generation-with-context`: 出题 API 需要扩展，支持通过 `knowledgeBaseId` 参数从知识库检索上下文

## Impact

- **后端新增依赖**: Apache PDFBox（PDF 解析）、Apache POI（Word 解析）、spring-ai-tika（文档解析）
- **数据库变更**: 新增 `t_knowledge_base`（知识库表）和 `t_document`（文档表）
- **向量存储**: 使用 Spring AI SimpleVectorStore（内存存储 + 文件持久化），无需额外基础设施
- **API 变更**: 出题接口 `POST /api/v1/quiz/create` 新增可选参数 `knowledgeBaseId`
- **前端新增页面**: 知识库管理页、文档上传组件
- **现有功能不受影响**: 原有的文本输入出题和联网搜索出题功能保持不变
