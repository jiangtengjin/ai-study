## ADDED Requirements

### Requirement: 基于知识库出题

系统 SHALL 支持用户选择一个知识库作为出题的知识来源。系统从知识库中检索与出题主题相关的文档片段，注入出题 Prompt，生成高度针对性的题目。

#### Scenario: 从知识库出题成功
- **WHEN** 用户输入出题主题并选择一个包含文档的知识库
- **THEN** 系统从该知识库中检索最相关的 5 个文档片段，注入出题 Prompt，AI 基于这些片段生成针对性题目

#### Scenario: 知识库无相关文档
- **WHEN** 用户选择的知识库为空（无文档）
- **THEN** 系统 SHALL 返回错误提示"该知识库暂无文档，请先上传文档后再出题"

#### Scenario: 知识库检索无相关结果
- **WHEN** 用户输入的出题主题与知识库中的文档内容无相关性（相似度低于阈值）
- **THEN** 系统 SHALL 降级到普通出题模式，使用用户输入的文本内容直接出题，并在日志中记录降级原因

### Requirement: 向量检索

系统 SHALL 使用向量相似度检索从知识库中查找与查询内容最相关的文档片段。

#### Scenario: 成功检索相关片段
- **WHEN** 系统对用户输入的出题主题进行向量检索
- **THEN** 系统返回相似度最高的 topK（默认 5）个文档片段，每个片段包含文本内容和相似度分数

#### Scenario: 设置相似度阈值
- **WHEN** 系统执行向量检索
- **THEN** 系统 SHALL 使用相似度阈值 0.7 过滤低质量结果，低于阈值的片段不纳入出题上下文

#### Scenario: 检索结果截断
- **WHEN** 检索到的文档片段总长度超过 3000 字符
- **THEN** 系统 SHALL 按相似度从高到低截断，确保注入 Prompt 的总内容不超过 3000 字符

### Requirement: RAG 出题 Prompt 注入

系统 SHALL 将检索到的文档片段以结构化方式注入出题 Prompt，指导 AI 基于文档内容出题。

#### Scenario: Prompt 包含 RAG 结果
- **WHEN** 系统构建出题 Prompt 且有 RAG 检索结果
- **THEN** Prompt 中包含以下结构：用户原始输入、RAG 检索结果（带 RAG_START/RAG_END 标记）、出题要求（优先参考 RAG 结果）

#### Scenario: Prompt 中无 RAG 结果
- **WHEN** RAG 检索无结果或用户未选择知识库
- **THEN** Prompt 仅包含用户原始输入和出题要求，与当前行为一致

### Requirement: 知识库与联网搜索互斥

系统 SHALL 确保知识库出题和联网搜索出题两种模式互斥，不可同时使用。

#### Scenario: 选择知识库时禁用联网搜索
- **WHEN** 用户选择了知识库 ID 且同时开启联网搜索
- **THEN** 系统 SHALL 优先使用知识库出题，忽略联网搜索设置

#### Scenario: 未选择知识库时使用联网搜索
- **WHEN** 用户未选择知识库 ID（为空）且开启联网搜索
- **THEN** 系统使用联网搜索获取知识，与当前行为一致

### Requirement: 出题结果与现有格式一致

系统 SHALL 确保基于知识库出题的结果 JSON 格式与现有出题结果完全一致。

#### Scenario: 输出格式一致性
- **WHEN** AI 使用 RAG 上下文生成题目
- **THEN** 输出的 JSON 格式与当前完全一致，包含 title、questions[] 数组，每个题目包含 index、difficulty、score、question、optionA-D、answer、explanation、knowledgePoint 字段

#### Scenario: 题目数量准确性
- **WHEN** 用户请求从知识库生成 10 道题
- **THEN** AI SHALL 生成恰好 10 道题，不因 RAG 上下文的加入而改变数量
