# OpenSpec 使用教程 - 傻瓜版

## 什么是 OpenSpec？

OpenSpec 是一个**规范驱动开发框架**，让你和 AI 先达成共识"要做什么"，然后再写代码。

简单说：**先规划，再动手**。

---

## 核心理念

```
传统方式：你说一句 → AI 直接写代码 → 可能不是你想要的
OpenSpec：你说想法 → AI 生成规范 → 你确认 → AI 再写代码 → 结果更准确
```

---

## 三分钟快速开始

### 第一步：安装 OpenSpec

```bash
npm install -g @fission-ai/openspec@latest
```

### 第二步：初始化项目

```bash
cd D:\JAVA\ai-study
openspec init
```

初始化后会创建这样的目录结构：

```
openspec/
├── specs/          # 规范（描述系统当前的行为）
├── changes/        # 变更（每个新功能一个文件夹）
└── config.yaml     # 配置文件（可选）
```

### 第三步：开始使用

在 Claude Code 中输入：

```
/opsx:propose 我想要添加的功能
```

就这么简单！

---

## 常用命令速查表

| 命令 | 用途 | 什么时候用 |
|------|------|-----------|
| `/opsx:propose` | 创建新功能的完整规范 | 有明确想法时 |
| `/opsx:explore` | 探索和讨论想法 | 想法还不清晰时 |
| `/opsx:apply` | 根据规范实现代码 | 规范确认后 |
| `/opsx:archive` | 归档已完成的功能 | 功能做完后 |

---

## 实际使用示例

### 场景：给项目添加"暗黑模式"

#### 方法一：一步到位（推荐新手）

```
你：/opsx:propose 添加暗黑模式

AI：已创建 openspec/changes/add-dark-mode/
    ✓ proposal.md — 为什么要做、做什么
    ✓ specs/       — 需求和场景
    ✓ design.md    — 技术方案
    ✓ tasks.md     — 实现清单
    
    准备就绪，运行 /opsx:apply 开始实现。
```

#### 方法二：先探索再决定

```
你：/opsx:explore 如何实现暗黑模式？

AI：让我看看你现在的项目...
    [分析代码]
    
    你有几种选择：
    1. CSS 变量方案（推荐，简单）
    2. 主题切换方案（更灵活）
    3. 第三方库方案（快速但不够灵活）
    
    你倾向哪种？

你：选方案1，开始吧

你：/opsx:propose add-dark-mode

AI：[生成规范文件...]
```

---

## OpenSpec 的文件结构

当你用 `/opsx:propose` 创建一个功能后，会生成：

```
openspec/changes/add-dark-mode/
├── proposal.md    # 这个功能的"提案"
├── specs/         # 详细的需求规范
├── design.md      # 技术设计文档
└── tasks.md       # 任务清单（实现时会用到）
```

### proposal.md - 提案

说明**为什么**要做这个功能、**做什么**、**范围**是什么。

```markdown
# 提案：添加暗黑模式

## 目标
减少用户夜间使用时的眼睛疲劳。

## 范围
- 在设置中添加主题切换
- 支持跟随系统设置
- 用 localStorage 保存用户选择
```

### tasks.md - 任务清单

```markdown
# 任务

## 1. 主题基础设施
- [ ] 1.1 创建主题上下文
- [ ] 1.2 添加 CSS 变量
- [ ] 1.3 实现本地存储

## 2. UI 组件
- [ ] 2.1 创建切换按钮
- [ ] 2.2 放到设置页面
```

---

## 完整工作流程

```
┌─────────────────────────────────────────────────────────────┐
│  1. 想法产生                                                  │
│     ↓                                                         │
│  2. /opsx:propose "我的功能"                                   │
│     ↓                                                         │
│  3. AI 生成规范（proposal, specs, design, tasks）                │
│     ↓                                                         │
│  4. 你审阅规范，确认或修改                                        │
│     ↓                                                         │
│  5. /opsx:apply                                               │
│     ↓                                                         │
│  6. AI 根据规范实现代码                                         │
│     ↓                                                         │
│  7. 测试、验证                                                  │
│     ↓                                                         │
│  8. /opsx:archive                                             │
│     ↓                                                         │
│  9. 功能完成，规范归档                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 常见问题

### Q: 我只是想加个小功能，也要用 OpenSpec 吗？

A: 不一定。OpenSpec 适合：
- 复杂功能（需要多文件修改）
- 不确定的需求（需要先探索）
- 团队协作（需要文档对齐）

小改动（改个按钮颜色、修个错别字）直接用 AI 就行。

### Q: 生成的规范不满意怎么办？

A: 直接修改文件！OpenSpec 的文件都是 Markdown，你可以手动编辑，然后继续 `/opsx:apply`。

### Q: 可以跳过某些步骤吗？

A: 可以。你可以：
- 直接编辑 `tasks.md` 删除不想做的任务
- 修改 `proposal.md` 调整范围
- 跳过 `/opsx:archive` 不归档

OpenSpec 是**灵活的**，不是死板的流程。

---

## 在 ai-study 项目中使用

现在你可以这样给项目扩展功能：

```bash
# 1. 先初始化
cd D:\JAVA\ai-study
openspec init

# 2. 在 Claude Code 中使用
/opsx:propose 添加学习排行榜功能
```

AI 会生成：
- 排行榜的需求规范
- 数据库设计
- API 设计
- 前端页面设计
- 实现任务清单

你确认后，AI 就能按规范实现代码。

---

## 总结

| 步骤 | 命令 | 作用 |
|------|------|------|
| 想法 → 规范 | `/opsx:propose` | 让 AI 理解你要做什么 |
| 规范 → 代码 | `/opsx:apply` | 让 AI 按规范写代码 |
| 完成 → 归档 | `/opsx:archive` | 保存规范以备后用 |

**核心思想**：先让 AI 生成"蓝图"，你确认"蓝图"没问题，AI 再按"蓝图"施工。

这样做的好处：
- ✅ AI 更准确理解你的需求
- ✅ 减少返工
- ✅ 有文档可追溯
- ✅ 团队协作更顺畅

---

## 更多资源

- [OpenSpec GitHub](https://github.com/Fission-AI/OpenSpec)
- [官方文档](https://github.com/Fission-AI/OpenSpec/tree/main/docs)
- [支持的 AI 工具](https://github.com/Fission-AI/OpenSpec/blob/main/docs/supported-tools.md)
