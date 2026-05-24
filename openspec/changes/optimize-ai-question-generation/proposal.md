## Why

当前 AI 出题功能完全依赖大模型的训练数据。当用户输入较新或较冷门的知识主题时（如 "Harness Engineering"），AI 可能给出错误的题目和解析，甚至混淆为其他领域的知识。这直接损害了产品的核心价值——学习准确性。需要在出题前引入网络搜索获取真实知识，确保题目基于事实而非模型幻觉。

此外，当前项目的 AI 调用层通过 `RestTemplate` 手动封装，缺乏抽象和可扩展性。借此次改造机会，引入 Spring AI Alibaba 框架重构 AI 调用层，为后续功能扩展（RAG、Agent 等）奠定基础。

## What Changes

- **引入 Spring AI Alibaba 框架**：替换手动 RestTemplate 调用，使用 Spring AI 的 ChatClient 统一管理 AI 模型调用
- **新增 Tavily 知识检索服务**：通过 Tavily Search API 搜索关键词获取知识，通过 Tavily Extract API 提取网页 URL 的完整内容
- **联网搜索为可选功能**：用户在首页主动开启"联网搜索"后才触发搜索，首页已有 UI 占位
- **改造出题流程**：用户输入主题 → （可选）Tavily 搜索/提取知识 → 将结果作为上下文传给 AI → AI 基于真实知识生成题目
- **优化出题 Prompt**：增加对参考知识的引用要求，提升题目准确性

## Capabilities

### New Capabilities
- `spring-ai-alibaba-refactor`: Spring AI Alibaba 框架重构——将现有 AiService 从 RestTemplate 手动调用改造为 Spring AI ChatClient，支持 DashScope/DeepSeek 模型
- `knowledge-retrieval`: Tavily 知识检索能力——支持关键词搜索（Tavily Search API）和 URL 内容提取（Tavily Extract API），作为可选增强功能
- `question-generation-with-context`: 基于检索知识的出题能力——将搜索/提取的知识作为上下文注入出题 Prompt，确保 AI 基于事实生成题目

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **后端代码**：重构 `AiService`（引入 ChatClient），新增 `KnowledgeService`（Tavily 搜索+提取），改造 `QuizService.createSession()` 流程
- **配置文件**：新增 Spring AI Alibaba、Tavily API 配置
- **依赖**：新增 `spring-ai-alibaba-starter`、`spring-ai-tavily` Maven 依赖
- **API 接口**：`POST /api/v1/quiz/create` 请求体新增可选 `enableSearch` 参数，响应不变
- **用户体验**：开启联网搜索时出题前增加搜索步骤（1-3 秒），题目质量显著提升；不开启则与原流程一致
