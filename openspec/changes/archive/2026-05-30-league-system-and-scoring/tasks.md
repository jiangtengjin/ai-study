## 1. 数据库与实体层

- [x] 1.1 创建 t_league_tier 段位配置表（id, name, icon, min_points, sort_order）
- [x] 1.2 创建 t_league_group 联赛小组表（id, tier_id, week_start_date, status）
- [x] 1.3 创建 t_league_member 小组成员表（id, group_id, user_id, weekly_points, ranking, result）
- [x] 1.4 创建 t_weekly_score 周积分明细表（id, user_id, session_id, points, week_start_date）
- [x] 1.5 t_user 表新增 total_points 字段（BIGINT DEFAULT 0）
- [x] 1.6 创建对应的 MyBatis Plus Entity 类（LeagueTier, LeagueGroup, LeagueMember, WeeklyScore）
- [x] 1.7 创建对应的 Mapper 接口
- [x] 1.8 初始化段位配置数据（8 级段位 INSERT）

## 2. 积分计算服务

- [x] 2.1 创建 PointCalculator 工具类，实现单题积分计算逻辑（easy/medium/hard/balanced）
- [x] 2.2 修改 QuizService.finishSession，集成会话积分汇总
- [x] 2.3 实现积分双写：累加 t_user.total_points + 写入 t_weekly_score
- [x] 2.4 创建 ScoreService，提供积分查询、积分明细等方法

## 3. 段位服务

- [x] 3.1 创建 TierService，实现段位判定逻辑（根据 total_points 查 t_league_tier）
- [x] 3.2 创建 LeagueTierController，实现 GET /api/v1/league/tier 接口
- [x] 3.3 创建 LeagueTierVO 返回当前段位、下一等级、积分差

## 4. 周联赛分组

- [x] 4.1 创建 LeagueGroupService，实现分组核心逻辑
- [x] 4.2 实现分层分组算法：同段位用户按近 4 周平均周积分排序后分组
- [x] 4.3 实现人数不足分档处理（30+/15-29/10-14/<10）
- [x] 4.4 创建 ScheduledTask，周一 00:00 自动执行分组
- [x] 4.5 实现中途加入用户等下周逻辑（首次答题时不分配小组）

## 5. 周积分与排名

- [x] 5.1 实现周积分实时更新：答题后更新 t_league_member.weekly_points
- [x] 5.2 集成 Redis Sorted Set 缓存周排行榜（ZADD/ZRANGE）
- [x] 5.3 创建 LeagueRankingController，实现 GET /api/v1/league/ranking 接口
- [x] 5.4 创建 LeagueRankingVO 返回小组排名列表、用户排名、距晋级线积分差

## 6. 联赛结算

- [x] 6.1 创建 LeagueSettlementService，实现结算核心逻辑
- [x] 6.2 实现晋级逻辑：Top 10 且非最高段位 → 段位 +1
- [x] 6.3 实现保级逻辑：排名 11-25 → 段位不变
- [x] 6.4 实现降级逻辑：Bottom 5 且非最低段位 → 段位 -1
- [x] 6.5 实现周积分 0 直接降级逻辑
- [x] 6.6 实现边界处理：最高段位不晋级、最低段位不降级、不足 15 人不降级
- [x] 6.7 创建 ScheduledTask，周日 23:59 自动执行结算
- [x] 6.8 实现结算结果写入 t_league_member.result

## 7. 联赛历史与通知

- [x] 7.1 创建 LeagueHistoryController，实现 GET /api/v1/league/history 接口
- [x] 7.2 实现近 12 周联赛历史查询
- [x] 7.3 创建结算结果 VO（晋级/保级/降级通知内容）

## 8. 趋势统计扩展

- [x] 8.1 修改 TrendStatsService，新增积分趋势查询
- [x] 8.2 修改 TrendStatsVO，新增 pointsEarned 字段
- [x] 8.3 修改 QuizAnswerMapper，聚合查询增加积分维度
