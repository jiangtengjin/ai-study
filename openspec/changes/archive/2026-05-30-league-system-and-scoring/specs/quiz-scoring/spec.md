## ADDED Requirements

### Requirement: 单题积分计算

系统 SHALL 根据题目难度和答题正误计算单题积分。

#### Scenario: 答对 easy 题目

- **WHEN** 用户答对一道 easy 难度的题目
- **THEN** 获得 10 积分

#### Scenario: 答错 easy 题目

- **WHEN** 用户答错一道 easy 难度的题目
- **THEN** 获得 2 积分

#### Scenario: 答对 medium 题目

- **WHEN** 用户答对一道 medium 难度的题目
- **THEN** 获得 20 积分

#### Scenario: 答错 medium 题目

- **WHEN** 用户答错一道 medium 难度的题目
- **THEN** 获得 5 积分

#### Scenario: 答对 hard 题目

- **WHEN** 用户答对一道 hard 难度的题目
- **THEN** 获得 35 积分

#### Scenario: 答错 hard 题目

- **WHEN** 用户答错一道 hard 难度的题目
- **THEN** 获得 8 积分

#### Scenario: balanced 难度会话中的题目

- **WHEN** 用户完成一道 balanced 难度会话中的题目
- **THEN** 系统按该题自身的 difficulty 字段（easy/medium/hard）计算积分

### Requirement: 会话积分汇总

系统 SHALL 在答题会话结束时计算本次会话的总积分。

#### Scenario: 会话结束计算积分

- **WHEN** 用户完成一次答题会话（调用 finishSession）
- **THEN** 系统汇总本次会话所有题目的积分，得到本次会话积分

#### Scenario: 积分双写

- **WHEN** 会话积分计算完成
- **THEN** 系统同时将积分累加到用户的累计总积分（t_user.total_points）和本周积分（t_weekly_score）

### Requirement: 积分记录明细

系统 SHALL 保存每次会话的积分明细记录。

#### Scenario: 查询积分历史

- **WHEN** 用户请求积分明细
- **THEN** 系统返回每次会话的积分、题目数量、正确率、难度、时间
