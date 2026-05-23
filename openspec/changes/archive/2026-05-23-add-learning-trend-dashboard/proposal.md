## Why

用户目前只能查看当前的学习统计数据（连续天数、总题数、正确率），但无法直观地看到自己的学习趋势和进步轨迹。添加学习趋势数据统计看板可以帮助用户：
- 了解长期学习效果和进步情况
- 发现学习中的薄弱环节
- 保持学习动力和成就感

## What Changes

- 新增后端 API：获取用户学习趋势数据（按日/周/月统计）
- 新增前端页面：学习趋势看板，包含图表展示
- 新增数据库查询：聚合统计用户的答题数据

## Capabilities

### New Capabilities
- `learning-trend-stats`: 学习趋势数据统计功能，包括按时间维度聚合答题数据、计算正确率趋势、学习时长统计等
- `trend-dashboard-ui`: 学习趋势看板前端页面，使用图表展示学习数据趋势

### Modified Capabilities

（无现有规范需要修改）

## Impact

- **后端代码**：
  - 新增 `TrendStatsController` 和 `TrendStatsService`
  - 修改 `QuizRecordRepository` 添加统计查询方法
- **前端代码**：
  - 新增 `TrendDashboard.vue` 页面
  - 新增 `trend.ts` API 接口文件
  - 修改路由配置添加新页面
- **依赖**：
  - 前端需要引入图表库（如 Chart.js 或 ECharts）
