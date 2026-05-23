## ADDED Requirements

### Requirement: 获取学习趋势数据

系统 SHALL 提供 API 接口，按时间维度聚合用户的学习数据。

#### Scenario: 按天统计

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=day&days=7`
- **THEN** 系统返回最近7天每天的答题数量、正确率、学习时长

#### Scenario: 按周统计

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=week&days=30`
- **THEN** 系统返回最近30天按周聚合的答题数量、正确率、学习时长

#### Scenario: 按月统计

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=month&days=90`
- **THEN** 系统返回最近90天按月聚合的答题数量、正确率、学习时长

### Requirement: 统计数据字段

系统 SHALL 返回以下统计数据字段。

#### Scenario: 返回字段完整性

- **WHEN** 系统返回趋势统计数据
- **THEN** 每个时间点包含以下字段：
  - `date`: 日期（格式：YYYY-MM-DD）
  - `totalQuestions`: 答题总数
  - `correctAnswers`: 正确答题数
  - `accuracy`: 正确率（百分比，保留两位小数）
  - `studyDuration`: 学习时长（分钟）

### Requirement: 查询参数验证

系统 SHALL 验证请求参数的有效性。

#### Scenario: 无效的 period 参数

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=invalid`
- **THEN** 系统返回 400 错误，提示 period 参数无效

#### Scenario: 无效的 days 参数

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=day&days=100`
- **THEN** 系统返回 400 错误，提示 days 参数超出范围

### Requirement: 用户认证

系统 SHALL 要求用户登录才能访问趋势数据。

#### Scenario: 未登录访问

- **WHEN** 未登录用户请求 `GET /api/v1/trend/stats`
- **THEN** 系统返回 401 未授权错误

#### Scenario: 已登录访问

- **WHEN** 已登录用户请求 `GET /api/v1/trend/stats`
- **THEN** 系统返回该用户的学习趋势数据
