## Context

当前 AI 知识闯关平台的出题流程为：用户输入文本/URL → 联网搜索补充知识 → AI 生成题目。这种方式生成的题目是通用化的，无法针对特定学科、课程或考试进行定制。

用户的核心诉求是：上传自己的课件、笔记、教材等文档，系统基于这些文档生成高度针对性的题目，用于考前突击复习。

技术现状：
- Spring Boot 3.3.6 + Spring AI 1.0.0-M6（里程碑版本）
- DeepSeek 通过 OpenAI 兼容接口接入
- MySQL 8.0 + MyBatis-Plus 3.5.6
- 无向量数据库基础设施

## Goals / Non-Goals

**Goals:**
- 支持用户上传 PDF、Word、TXT 文档到知识库
- 基于文档内容进行向量检索，将相关片段注入出题 Prompt
- 用户可以管理多个知识库（创建、查看、删除）
- 出题时可选择知识库，生成高度针对性的题目
- 保持现有文本出题和联网搜索出题功能不变

**Non-Goals:**
- 不引入外部向量数据库（如 Chroma、Milvus），使用 Spring AI 内置 SimpleVectorStore
- 不实现文档在线编辑功能
- 不实现知识库共享或多用户协作
- 不升级 Spring Boot 大版本（保持 3.3.6，避免引入破坏性变更）

## Decisions

### Decision 1: 使用 SimpleVectorStore + 文件持久化

**选择**: Spring AI 内置的 `SimpleVectorStore`，通过 JSON 文件持久化向量数据。

**理由**:
- 项目当前无 Redis 或外部向量数据库，引入新基础设施增加运维复杂度
- SimpleVectorStore 是 Spring AI 的内置组件，零额外依赖
- 通过 `save(File)` / `load(File)` 实现文件级持久化，满足本地开发和小规模部署需求
- 每个知识库对应一个独立的 JSON 向量文件，便于管理和清理

**备选方案**:
- Chroma: 需要额外部署 Chroma 服务，运维成本高
- PgVector: 需要 PostgreSQL，与现有 MySQL 技术栈不一致
- MySQL 全文搜索: 无法实现语义相似度检索，效果差

### Decision 2: 使用 Apache Tika 进行文档解析

**选择**: 通过 `spring-ai-tika-document-reader` 依赖使用 Apache Tika 解析文档。

**理由**:
- Tika 是 Spring AI 官方推荐的文档解析方案
- 单一依赖支持 PDF、DOC/DOCX、TXT、PPT、Excel 等多种格式
- 与 Spring AI 的 `Document` 模型无缝集成

**备选方案**:
- 手动集成 PDFBox + POI: 需要分别引入两个依赖，代码量大
- 纯文本上传: 功能受限，用户体验差

**注意**: `spring-ai-tika-document-reader` 需要 Spring AI 1.0.0-M4+ 版本支持，当前项目版本 (1.0.0-M6) 满足要求。

### Decision 3: 文档分块策略

**选择**: 使用 Spring AI 的 `TokenTextSplitter` 进行文本分块，默认配置：
- chunkSize: 800 tokens
- minChunkSize: 200 tokens
- chunkOverlap: 200 tokens

**理由**:
- TokenTextSplitter 是 Spring AI 内置的分块器，基于 token 计数
- 800 tokens 的块大小适合出题场景，既能保留足够的上下文，又不会超出 LLM 的上下文窗口
- 200 tokens 的重叠确保相邻块之间的语义连贯性

### Decision 4: Embedding 模型选择

**选择**: 使用 DeepSeek 的 embedding API（如果支持），否则使用 OpenAI 兼容的 embedding 接口。

**理由**:
- 项目已接入 DeepSeek，复用现有配置减少复杂度
- DeepSeek 支持 OpenAI 兼容的 embedding 接口
- 如果 DeepSeek 不支持 embedding，可降级使用本地轻量级 embedding 方案

**备选方案**:
- 本地 embedding（如 sentence-transformers）: 需要 Python 环境，与 Java 项目不兼容
- 其他云服务（如通义千问 embedding）: 增加额外 API Key 配置

### Decision 5: 知识库与向量存储的关联

**选择**: 每个知识库对应一个独立的 VectorStore 实例，通过 `knowledgeBaseId` 作为 metadata filter 进行隔离。

**理由**:
- SimpleVectorStore 支持 `filterExpression` 进行元数据过滤
- 每个知识库的文档在存储时添加 `knowledgeBaseId` 元数据
- 查询时通过 filter 限定只检索指定知识库的文档

### Decision 6: 出题 Prompt 结构

**选择**: 在现有出题 Prompt 基础上，新增 RAG 检索结果注入段。

**结构**:
```
## 用户输入的知识内容
{content}

## 知识库检索的相关知识点（如果有）
<<<RAG_START>>
{ragResults}
<<<RAG_END>>>

请优先参考"知识库检索的相关知识点"来出题，确保题目紧扣文档内容。
```

**理由**:
- 与现有联网搜索结果注入模式一致，降低实现复杂度
- 明确指示 AI 优先使用 RAG 结果，确保题目针对性
- 保留降级能力：RAG 无结果时回退到普通出题模式

## Risks / Trade-offs

### Risk 1: SimpleVectorStore 性能瓶颈
**影响**: 知识库文档量大时，内存占用高，检索变慢。
**缓解**: 限制单个知识库最多 50 个文档，单文档最大 10MB。超出时提示用户拆分。

### Risk 2: Embedding API 调用成本
**影响**: 每次上传文档都需要调用 embedding API，产生额外费用。
**缓解**: 文档上传时一次性向量化并持久化，后续出题只做检索不做重复 embedding。控制单次 embedding 的文本长度。

### Risk 3: 文档解析质量
**影响**: 复杂排版的 PDF/Word 可能解析出乱码或丢失结构。
**缓解**: Tika 对常见格式支持良好；对于解析失败的情况，返回友好错误提示引导用户重新上传或粘贴文本。

### Risk 4: Spring AI 版本为里程碑版本
**影响**: 1.0.0-M6 是预发布版本，API 可能存在不稳定性。
**缓解**: 封装向量存储操作到独立 Service，便于后续升级时只修改一个类。记录使用的 API 以便版本升级时排查。

## Migration Plan

1. **数据库变更**: 执行新增表的 SQL（t_knowledge_base、t_document），无破坏性变更
2. **依赖变更**: pom.xml 新增 spring-ai-tika-document-reader 依赖
3. **配置变更**: application.yml 新增文件上传和向量存储路径配置
4. **向后兼容**: 现有出题流程不受影响，knowledgeBaseId 为可选参数
5. **回滚策略**: 新增表和依赖可独立移除，不影响现有功能

## Open Questions

1. DeepSeek 是否支持 embedding API？如果不支持，需要选择替代的 embedding 服务
2. SimpleVectorStore 的 JSON 文件在大量文档时的读写性能是否可接受？需要实测验证
3. 是否需要支持文档的增量更新（追加内容到已有文档）？当前设计仅支持完整上传和删除
