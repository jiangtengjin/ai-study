## Context

当前系统有两个核心问题：

**问题一：AI 出题依赖训练数据，新/冷门主题出题错误**
出题流程：用户输入知识内容 → 直接传给 DeepSeek API → AI 基于训练数据生成题目。当用户输入较新或冷门的主题时（如 "Harness Engineering"），AI 的训练数据可能不包含该主题，导致混淆为其他领域的知识。

**问题二：AI 调用层缺乏抽象**
当前 `AiService` 通过 `RestTemplate` 手动封装 DeepSeek API 调用，无超时配置、无模型切换能力、无 Tool Calling 支持。后续如需扩展 RAG、Agent 等功能，改造成本高。

## Goals / Non-Goals

**Goals:**

- 引入 Spring AI Alibaba 框架重构 AI 调用层，替换 RestTemplate 手动调用
- 通过 Tavily Search API 支持关键词搜索获取知识
- 通过 Tavily Extract API 支持 URL 网页内容提取
- 联网搜索为可选功能，用户在首页主动开启后才触发（首页已有 UI 占位）
- 搜索结果作为上下文注入出题 Prompt，确保 AI 基于事实生成题目
- 搜索失败时自动降级为纯模型出题，不影响核心流程

**Non-Goals:**

- 不实现完整的 RAG 系统（向量数据库、Embedding 等）
- 不缓存搜索结果（后续迭代再考虑）
- 不替换现有 DeepSeek 模型——通过 Spring AI 的 OpenAI 兼容接口继续使用 DeepSeek

## Decisions

### Decision 1: AI 框架 — Spring AI Alibaba

**选择**: 引入 Spring AI Alibaba，使用 `spring-ai-alibaba-starter`

**理由**:

- Spring 官方生态，与 Spring Boot 3.2 原生集成
- 支持 DashScope（阿里云灵积）平台，可接入通义千问、DeepSeek 等模型
- 提供 ChatClient 统一 API，支持 Tool Calling、结构化输出
- 内置 Tavily 搜索工具支持
- 后续可扩展 RAG、Agent 等能力

**改造要点**:

- 引入 `spring-ai-alibaba-starter` 依赖
- 通过 OpenAI 兼容接口直连 DeepSeek（`spring.ai.openai.base-url=https://api.deepseek.com`），与当前接入方式一致，改动最小
- 将 `AiService` 中的 `RestTemplate` 调用替换为 `ChatClient` 调用
- 保留现有 Prompt 模板，通过 Spring AI 的 `Resource` 加载

**备选方案**:

- DashScope 统一接入：通过阿里云灵积平台中转，支持多模型统一管理，但多一层中转，且需要额外注册 DashScope 账号
- LangChain4j：社区驱动，模型支持更广，但非 Spring 官方，集成深度不如 Spring AI
- 维持现状（RestTemplate）：短期简单，但后续扩展成本高

### Decision 2: 搜索引擎 — Tavily Search API + Extract API

**选择**: 使用 Tavily 作为知识检索服务，同时使用其 Search 和 Extract 两个 API

**Tavily Search API**（`/search`）:

- 根据关键词搜索网络内容
- 返回 AI 优化的结构化结果（标题、摘要、URL）
- 适合：用户输入主题关键词的场景

**Tavily Extract API**（`/extract`）:

- 根据 URL 提取网页完整内容
- 自动清理广告、导航等无关内容，返回干净的 Markdown 文本
- 适合：用户输入网页链接的场景

**选择理由**:

- 专为 AI/RAG 场景设计，返回内容质量高
- Search + Extract 双 API 覆盖关键词搜索和 URL 提取两种需求
- Spring AI 内置 Tavily 工具支持，集成简单

**备选方案**:

- Bing Search API：仅支持关键词搜索，不支持 URL 内容提取；返回内容为摘要片段，不如 Tavily 适合 AI 场景
- Jsoup 自行抓取：需要处理反爬、JavaScript 渲染等问题，维护成本高

### Decision 3: 搜索触发方式 — 用户可选

**选择**: 联网搜索为可选功能，用户在首页主动开启后才触发

**实现方式**:

- `CreateQuizRequest` 新增 `enableSearch` 布尔参数（默认 false）
- 前端首页已有"联网搜索"UI 占位，用户点击开启后传入 `enableSearch=true`
- 后端根据该参数决定是否调用 KnowledgeService

**理由**:

- 用户对搜索延迟有预期（主动开启时）
- 节省 Tavily API 调用额度
- 搜索有时效性，不是所有主题都需要

### Decision 4: 知识获取策略 — 智能判断输入类型

**选择**: KnowledgeService 根据用户输入内容自动判断使用 Search API 还是 Extract API

**判断逻辑**:

```
用户输入内容
    │
    ├── 包含 URL（http/https 开头）→ 调用 Tavily Extract API 提取网页内容
    │
    └── 纯文本/关键词 → 调用 Tavily Search API 搜索相关内容
```

**理由**:

- 用户无需手动选择搜索模式
- URL 输入自动提取，体验更自然
- 两种模式互斥，不会同时调用

### Decision 5: Prompt 注入方式 — 上下文拼接

**选择**: 将搜索/提取结果注入出题 Prompt 的参考知识区域

**Prompt 结构**:

```
你是一个专业的教育出题专家。请根据以下知识内容生成 {count} 道高质量的选择题。

## 用户输入的知识内容
{content}

## 联网搜索获取的参考知识（如有）
{searchResults}

## 出题要求
...
请优先参考"联网搜索获取的参考知识"中的事实信息来出题。
如果搜索结果与用户输入主题不相关，请以用户输入为准。
```

**理由**:

- 简单直接，不需要 RAG 架构
- Spring AI ChatClient 支持 Prompt 模板，注入方便
- 搜索结果直接作为事实依据，减少 AI 幻觉

### Decision 6: 降级策略 — 搜索失败时 fallback

**选择**: Tavily API 调用失败（超时、限流、异常）时，跳过搜索步骤，使用原始用户输入直接出题

**理由**:

- 保证核心流程不被搜索服务阻断
- 用户体验不受影响（只是题目质量可能下降）
- 可通过日志监控搜索失败率

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Tavily 国内访问需要代理 | 搜索功能不可用 | 开发阶段通过本地代理（veee）访问；上线后需配置服务器代理或考虑替代方案 |
| Tavily API 限流/不可用 | 出题变慢或失败 | 降级到纯 AI 出题 + 日志告警 |
| 搜索结果质量差 | 生成的题目仍可能不准确 | AI 辅助判断相关性 + 用户反馈机制 |
| Spring AI Alibaba 引入风险 | 框架兼容性问题 | 逐步迁移，先替换 AiService，再扩展功能 |
| 出题延迟增加 | 用户体验下降 | 搜索超时控制（5 秒）+ 仅在用户开启时触发 |
| Prompt 过长 | 超出模型上下文限制 | 截断搜索/提取结果，保留前 3000 字符 |

## Migration Plan

**Phase 1: 引入 Spring AI Alibaba**

1. 添加 `spring-ai-alibaba-starter` Maven 依赖
2. 配置 OpenAI 兼容接口（`spring.ai.openai.api-key` + `base-url=https://api.deepseek.com`）
3. 将 `AiService` 中 RestTemplate 调用替换为 ChatClient 调用
4. 验证现有出题功能正常

**Phase 2: 新增 Tavily 知识检索**

1. 配置 Tavily API Key；开发阶段通过本地代理（veee）访问，JVM 参数加 `-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=代理端口`
2. 实现 `KnowledgeService`（Search + Extract 双模式）
3. 改造 `QuizService.createSession()` 流程，根据 `enableSearch` 参数决定是否调用
4. 改造出题 Prompt 模板，注入搜索结果上下文

**Phase 3: 前端适配**

1. 首页"联网搜索"开关接入 `enableSearch` 参数
2. 开启搜索时显示搜索中状态提示

**回滚方案**:

- Phase 1 回滚：恢复 RestTemplate 调用方式
- Phase 2 回滚：将 `KnowledgeService` 调用注释掉即可恢复原流程
- 联网搜索默认关闭，不影响现有用户体验

## Open Questions

> 已全部解决，记录如下：

1. ~~**DeepSeek 接入方式**~~ → **已决定：OpenAI 兼容接口直连 DeepSeek**，与当前接入方式一致，改动最小
2. ~~**Tavily 代理配置**~~ → **已决定：开发阶段使用本地代理（veee）**，上线后再配置服务器代理
3. ~~**Tavily API 额度**~~ → **已决定：现阶段免费层 1000 次/月够用**，后续上线再考虑付费
