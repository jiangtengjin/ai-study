# AI 知识闯关平台

一个基于 AI 的知识学习与竞技平台。用户粘贴任意知识内容，AI 自动生成选择题进行闯关答题，支持自建题库（RAG）、联网搜索增强出题，并通过周赛联赛系统实现段位升降的竞技排名。

## 解决什么问题

- **被动学习效率低**：传统看书记笔记容易遗忘，通过 AI 出题 + 主动答题的方式强化记忆
- **题目来源受限**：不需要预制题库，粘贴知识文本即可由 AI 实时生成高质量选择题
- **缺乏学习动力**：联赛段位机制（铜牌 → 黑曜石，8 个段位）引入竞争元素，每周 30 人小组比拼，升降级驱动持续学习
- **知识掌握不自知**：AI 学习报告自动分析薄弱知识点，错题本归集易错题，精准查漏补缺

## 核心功能

### AI 出题闯关

粘贴知识文本 → 选择题量（5/10/15/20）和难度 → AI 生成选择题 → 逐题作答 → 查看解析与学习报告

三种出题模式：
- **纯文本模式**：直接基于用户输入出题
- **联网增强模式**：接入 Tavily 搜索 API，自动补充网络知识作为出题参考
- **RAG 增强模式**：基于自建知识库的文档向量检索，用已有资料精准出题

### 知识库 & RAG

创建知识库 → 上传文档（PDF/DOCX/TXT）→ 自动切片、向量化存储 → 出题时向量检索相关段落作为上下文

文档处理链路：Tika 解析 → 文本清洗 → TokenTextSplitter 分块（800 token / 200 重叠）→ 通义千问 embedding → SimpleVectorStore 持久化

### 联赛系统

- **8 个段位**：铜牌、银牌、金牌、蓝宝石、红宝石、紫水晶、珍珠、黑曜石
- **周赛制**：周一自动分组（同段位 30 人），周日结算升降级
- **匹配策略**：按近 4 周平均积分排序后均分，保证实力相近
- **升降规则**：前 33% 升段，后 17% 降段；低于 15 人的小组只升不降；零分强制降段

### 学习分析

- 学习报告：AI 生成知识掌握分析、薄弱点、学习建议
- 错题本：自动归集错题，支持复习和移除
- 趋势统计：日/周/月维度的正确率、答题量、学习时长图表

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| Java 21 | 语言 |
| Spring Boot 3.3.6 | 应用框架 |
| Spring AI 1.0.0-M6 | AI 集成（OpenAI 兼容协议） |
| MyBatis-Plus 3.5.6 | ORM |
| Sa-Token 1.38.0 | 认证鉴权（Redis 持久化会话） |
| MySQL | 数据库 |
| Redis | 会话存储 |
| Knife4j 4.5.0 | API 文档（OpenAPI 3） |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3.4 + TypeScript | UI 框架 |
| Vite 5.4 | 构建工具 |
| Element Plus 2.7 | 组件库 |
| ECharts 5.5 | 数据可视化 |
| Pinia 2.1 | 状态管理 |
| Axios 1.7 | HTTP 客户端 |

### AI 服务

| 服务 | 用途 |
|------|------|
| DeepSeek Chat | 出题生成、学习报告生成 |
| 通义千问 text-embedding-v3 | 文档向量化（RAG） |
| Tavily Search/Extract | 联网知识检索 |

## 项目结构

```
ai-study/
├── ai-study-server/          # 主后端（Spring Boot + AI + Redis + 联赛）
├── ai-study-server-basic/    # 精简后端（JWT 认证，无 Redis/向量库）
├── ai-study-web/             # 前端（Vue 3 + Element Plus）
├── docs/                     # 需求分析与设计文档
└── prototypes/               # UI 原型
```

## 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 6.0+（主后端需要）
- Node.js 18+

### 后端启动

```bash
# 1. 创建数据库并导入表结构
mysql -u root -p < ai-study-server/src/main/resources/schema.sql

# 2. 修改数据库和 Redis 连接配置
#    ai-study-server/src/main/resources/application-dev.yml

# 3. 配置 API Key（DeepSeek、通义千问、Tavily）
#    ai-study-server/src/main/resources/application-dev.yml

# 4. 启动
cd ai-study-server
mvn spring-boot:run
```

### 前端启动

```bash
cd ai-study-web
npm install
npm run dev
```

访问 http://localhost:5173

### API 文档

启动后端后访问 Knife4j 文档：http://localhost:8080/swagger-ui.html

## 两种后端模式

| 特性 | ai-study-server（主） | ai-study-server-basic（精简） |
|------|----------------------|------------------------------|
| 认证方式 | Sa-Token + Redis Session | Sa-Token + JWT |
| AI 出题 | DeepSeek + Spring AI | 不支持 |
| RAG 知识库 | 通义千问 Embedding + 向量存储 | 不支持 |
| 联网搜索 | Tavily API | 不支持 |
| 联赛系统 | 完整 | 不支持 |
| 外部依赖 | MySQL + Redis | 仅 MySQL |
