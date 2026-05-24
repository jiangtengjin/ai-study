## ADDED Requirements

### Requirement: 关键词搜索知识

系统 SHALL 提供通过 Tavily Search API 根据关键词搜索知识的能力。搜索结果 SHALL 包含标题、摘要片段和来源 URL。

#### Scenario: 成功搜索知识
- **WHEN** 用户输入主题 "Harness Engineering" 并开启联网搜索
- **THEN** 系统调用 Tavily Search API 搜索该主题，返回最多 5 条相关搜索结果，每条包含 title、content、url

#### Scenario: 搜索结果为空
- **WHEN** 系统搜索某主题但 Tavily API 返回 0 条结果
- **THEN** 系统 SHALL 使用原始用户输入内容继续出题流程，并在日志中记录搜索无结果

#### Scenario: 搜索 API 调用失败
- **WHEN** Tavily API 返回错误（超时、限流、网络异常）
- **THEN** 系统 SHALL 降级到不使用搜索结果的出题流程，使用原始用户输入直接出题，并在日志中记录错误

#### Scenario: 搜索 API 超时
- **WHEN** Tavily API 响应超过 5 秒
- **THEN** 系统 SHALL 中断搜索请求，降级到原始出题流程

### Requirement: URL 内容提取

系统 SHALL 提供通过 Tavily Extract API 从 URL 提取网页完整内容的能力。提取结果 SHALL 为清理后的干净文本。

#### Scenario: 成功提取网页内容
- **WHEN** 用户输入包含 URL（如 "https://example.com/article"）并开启联网搜索
- **THEN** 系统调用 Tavily Extract API 提取该 URL 的完整内容，返回清理后的 Markdown 文本

#### Scenario: URL 提取失败
- **WHEN** Tavily Extract API 返回错误或无法访问该 URL
- **THEN** 系统 SHALL 降级到不使用提取结果的出题流程，使用原始用户输入直接出题

#### Scenario: 多个 URL 输入
- **WHEN** 用户输入包含多个 URL
- **THEN** 系统 SHALL 提取所有 URL 的内容，合并后作为参考知识

### Requirement: 智能判断输入类型

系统 SHALL 根据用户输入内容自动判断使用 Search API 还是 Extract API。

#### Scenario: 输入包含 URL
- **WHEN** 用户输入内容以 http:// 或 https:// 开头
- **THEN** 系统 SHALL 调用 Tavily Extract API 提取网页内容，不调用 Search API

#### Scenario: 输入为纯文本
- **WHEN** 用户输入内容不包含 URL
- **THEN** 系统 SHALL 调用 Tavily Search API 搜索相关内容，不调用 Extract API

### Requirement: 搜索为可选功能

联网搜索 SHALL 为可选功能，仅在用户主动开启时触发。

#### Scenario: 用户开启联网搜索
- **WHEN** 用户在首页开启"联网搜索"开关，请求中 `enableSearch=true`
- **THEN** 系统 SHALL 调用 KnowledgeService 获取知识

#### Scenario: 用户未开启联网搜索
- **WHEN** 用户未开启"联网搜索"开关，请求中 `enableSearch=false` 或未传该参数
- **THEN** 系统 SHALL 跳过 KnowledgeService，直接使用原始用户输入出题

### Requirement: 搜索配置管理

系统 SHALL 支持通过配置文件管理 Tavily API 的密钥和代理设置。

#### Scenario: 配置 Tavily API Key
- **WHEN** 管理员在 application.yml 中配置 Tavily API Key
- **THEN** 系统使用该配置调用 Tavily API

#### Scenario: 配置代理
- **WHEN** 服务器在国内，配置了 HTTP 代理
- **THEN** 系统通过代理访问 Tavily API
