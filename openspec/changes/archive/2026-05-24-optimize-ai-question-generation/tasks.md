## 1. Spring AI Alibaba 框架引入

- [x] 1.1 添加 `spring-ai-alibaba-starter` Maven 依赖到 pom.xml
- [x] 1.2 在 application.yml 中配置 Spring AI Alibaba（DashScope API Key 或 OpenAI 兼容接口）
- [x] 1.3 验证 Spring AI ChatClient 可正常注入和调用

## 2. AiService 重构

- [x] 2.1 将 `AiService` 中 RestTemplate 调用替换为 Spring AI ChatClient 调用
- [x] 2.2 使用 Spring AI 的 PromptTemplate 加载现有 Prompt 模板
- [x] 2.3 实现结构化输出（JSON → Map 的自动映射）
- [ ] 2.4 验证现有出题功能（不开启搜索）与原流程行为一致

## 3. Tavily 知识检索服务

- [x] 3.1 配置 Tavily API Key 和代理设置（application.yml）
- [x] 3.2 创建 `KnowledgeService` 类
- [x] 3.3 实现 `searchByKeyword(String query)` 方法——调用 Tavily Search API
- [x] 3.4 实现 `extractFromUrl(String url)` 方法——调用 Tavily Extract API
- [x] 3.5 实现输入类型判断逻辑——URL 开头走 Extract，纯文本走 Search
- [x] 3.6 实现超时控制（5 秒）和异常处理，失败时返回空结果
- [x] 3.7 实现搜索结果格式化——拼接为结构化文本供 Prompt 使用

## 4. 出题流程改造

- [x] 4.1 `CreateQuizRequest` 新增 `enableSearch` 布尔参数（默认 false）
- [x] 4.2 改造 `QuizService.createSession()`——根据 `enableSearch` 决定是否调用 KnowledgeService
- [x] 4.3 将搜索结果传入 AiService 出题调用
- [x] 4.4 改造出题 Prompt 模板——新增搜索结果上下文注入区域

## 5. 验证与测试

- [ ] 5.1 不开启搜索——验证出题流程与原流程一致
- [ ] 5.2 开启搜索+关键词输入——验证搜索结果注入后题目质量提升
- [ ] 5.3 开启搜索+URL 输入——验证网页内容提取并用于出题
- [ ] 5.4 测试搜索 API 超时/失败——验证降级到原始出题流程
- [ ] 5.5 测试搜索结果不相关——验证 AI 仍能基于用户输入生成题目
