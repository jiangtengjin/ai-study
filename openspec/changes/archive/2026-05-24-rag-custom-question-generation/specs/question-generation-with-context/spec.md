## MODIFIED Requirements

### Requirement: 基于搜索结果出题

系统 SHALL 将搜索/提取的知识或知识库检索结果作为上下文注入出题 Prompt，使 AI 基于真实知识生成题目而非仅依赖训练数据。系统 SHALL 支持两种知识来源：联网搜索和知识库 RAG 检索，两者互斥。

#### Scenario: 使用搜索结果出题
- **WHEN** 用户输入主题 "Harness Engineering" 且搜索返回了相关结果
- **THEN** 出题 Prompt 中包含用户原始输入和搜索结果摘要，AI 基于这些信息生成准确的题目

#### Scenario: 使用知识库 RAG 结果出题
- **WHEN** 用户选择一个知识库且输入出题主题
- **THEN** 出题 Prompt 中包含用户原始输入和从知识库检索的相关文档片段，AI 基于这些信息生成针对性题目

#### Scenario: 搜索结果不相关时的处理
- **WHEN** 搜索返回的结果与用户输入主题明显不相关
- **THEN** AI SHALL 在生成的题目中优先使用用户输入的知识内容，忽略不相关的搜索结果

#### Scenario: RAG 检索无相关结果时的处理
- **WHEN** 知识库中没有与出题主题相关的文档片段（相似度低于阈值）
- **THEN** 系统 SHALL 降级到使用用户输入内容直接出题，不注入 RAG 结果

### Requirement: 出题 Prompt 优化

出题 Prompt SHALL 包含明确的指令，要求 AI 优先参考搜索结果或 RAG 检索结果中的事实信息，并在结果与用户输入冲突时以检索结果为准。

#### Scenario: Prompt 包含搜索结果
- **WHEN** 系统构建出题 Prompt 且有搜索结果
- **THEN** Prompt 中包含以下结构：用户原始输入、搜索结果摘要、出题要求（优先参考搜索结果）

#### Scenario: Prompt 包含 RAG 检索结果
- **WHEN** 系统构建出题 Prompt 且有 RAG 检索结果
- **THEN** Prompt 中包含以下结构：用户原始输入、RAG 检索结果（带标记）、出题要求（优先参考 RAG 结果）

#### Scenario: Prompt 中无搜索结果也无 RAG 结果
- **WHEN** 搜索未返回结果且用户未选择知识库
- **THEN** Prompt 仅包含用户原始输入和出题要求，与当前行为一致

### Requirement: 出题质量保障

系统 SHALL 确保搜索结果或 RAG 结果注入后，出题的 JSON 格式和字段结构保持不变，不影响现有答题流程。

#### Scenario: 输出格式一致性
- **WHEN** AI 使用搜索结果或 RAG 结果上下文生成题目
- **THEN** 输出的 JSON 格式与当前完全一致，包含 title、questions[] 数组，每个题目包含 index、difficulty、score、question、optionA-D、answer、explanation、knowledgePoint 字段

#### Scenario: 题目数量准确性
- **WHEN** 用户请求生成 10 道题
- **THEN** AI SHALL 生成恰好 10 道题，不因搜索结果或 RAG 结果的加入而改变数量

### Requirement: Spring AI ChatClient 调用

系统 SHALL 使用 Spring AI 的 ChatClient 替代现有 RestTemplate 调用 AI 模型。

#### Scenario: ChatClient 调用成功
- **WHEN** 系统调用 AI 生成题目
- **THEN** 通过 Spring AI ChatClient 发送请求，返回结构化 JSON 响应

#### Scenario: ChatClient 调用失败重试
- **WHEN** AI 模型调用失败
- **THEN** 系统 SHALL 进行最多 3 次重试，间隔指数退避
