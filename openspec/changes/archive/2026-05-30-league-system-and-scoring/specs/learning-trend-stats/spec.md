## MODIFIED Requirements

### Requirement: 统计数据字段

系统 SHALL 返回以下统计数据字段，并新增积分相关字段。

#### Scenario: 返回字段完整性

- **WHEN** 系统返回趋势统计数据
- **THEN** 每个时间点包含以下字段：
  - `date`: 日期（格式：YYYY-MM-DD）
  - `totalQuestions`: 答题总数
  - `correctAnswers`: 正确答题数
  - `accuracy`: 正确率（百分比，保留两位小数）
  - `studyDuration`: 学习时长（分钟）
  - `pointsEarned`: 该时间段获得的积分

## ADDED Requirements

### Requirement: 积分趋势统计

系统 SHALL 提供积分维度的趋势数据。

#### Scenario: 按天查询积分趋势

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=day&days=7`
- **THEN** 系统返回最近 7 天每天的积分增长数据

#### Scenario: 按周查询积分趋势

- **WHEN** 用户请求 `GET /api/v1/trend/stats?period=week&days=30`
- **THEN** 系统返回最近 30 天按周聚合的积分增长数据
