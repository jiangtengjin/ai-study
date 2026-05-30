package com.aistudy.service;

import com.aistudy.entity.*;
import com.aistudy.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueGroupService {

    private final LeagueTierMapper leagueTierMapper;
    private final LeagueGroupMapper leagueGroupMapper;
    private final LeagueMemberMapper leagueMemberMapper;
    private final WeeklyScoreMapper weeklyScoreMapper;
    private final UserMapper userMapper;

    private static final int GROUP_SIZE = 30;
    private static final int MIN_GROUP_SIZE = 10;
    private static final int NO_DEMOTE_SIZE = 15;
    private static final double PROMOTE_RATIO = 0.33;
    private static final double DEMOTE_RATIO = 0.17;

    /**
     * 执行周一自动分组
     */
    @Transactional
    public void executeWeeklyGrouping() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        log.info("开始执行周联赛分组，周起始日期: {}", weekStart);

        // 检查本周是否已分组
        Long existCount = leagueGroupMapper.selectCount(
                new LambdaQueryWrapper<LeagueGroup>()
                        .eq(LeagueGroup::getWeekStartDate, weekStart));
        if (existCount > 0) {
            log.info("本周已分组，跳过");
            return;
        }

        // 获取所有段位
        List<LeagueTier> tiers = leagueTierMapper.selectList(
                new LambdaQueryWrapper<LeagueTier>().orderByAsc(LeagueTier::getSortOrder));

        for (LeagueTier tier : tiers) {
            groupByTier(tier, weekStart);
        }

        log.info("周联赛分组完成");
    }

    /**
     * 按段位分组
     */
    private void groupByTier(LeagueTier tier, LocalDate weekStart) {
        // 获取该段位的所有活跃用户（排除本周积分为0的）
        List<User> users = getUsersByTier(tier);
        if (users.isEmpty()) {
            log.info("段位 {} 无活跃用户，跳过分组", tier.getName());
            return;
        }

        // 按近4周平均周积分排序
        Map<Long, Double> avgScores = calculateAvgWeeklyScores(users, weekStart);
        users.sort((u1, u2) -> Double.compare(
                avgScores.getOrDefault(u2.getId(), 0.0),
                avgScores.getOrDefault(u1.getId(), 0.0)));

        // 分档处理
        int userCount = users.size();
        if (userCount < MIN_GROUP_SIZE) {
            log.info("段位 {} 人数不足 {}，不开组 (当前 {} 人)", tier.getName(), MIN_GROUP_SIZE, userCount);
            return;
        }

        // 创建小组
        List<List<User>> groups = partitionUsers(users);
        for (List<User> group : groups) {
            if (group.size() < MIN_GROUP_SIZE) {
                log.info("段位 {} 余下 {} 人不足 {}，不开组", tier.getName(), group.size(), MIN_GROUP_SIZE);
                continue;
            }
            createGroup(tier, group, weekStart);
        }
    }

    /**
     * 获取指定段位的用户（基于 tierId）
     */
    private List<User> getUsersByTier(LeagueTier tier) {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getTierId, tier.getId()));
    }

    /**
     * 计算用户近4周平均周积分
     */
    private Map<Long, Double> calculateAvgWeeklyScores(List<User> users, LocalDate currentWeekStart) {
        LocalDate fourWeeksAgo = currentWeekStart.minusWeeks(4);
        List<Long> userIds = users.stream().map(User::getId).toList();

        List<WeeklyScore> scores = weeklyScoreMapper.selectList(
                new LambdaQueryWrapper<WeeklyScore>()
                        .in(WeeklyScore::getUserId, userIds)
                        .ge(WeeklyScore::getWeekStartDate, fourWeeksAgo)
                        .lt(WeeklyScore::getWeekStartDate, currentWeekStart));

        Map<Long, List<WeeklyScore>> grouped = scores.stream()
                .collect(Collectors.groupingBy(WeeklyScore::getUserId));

        Map<Long, Double> result = new HashMap<>();
        for (Long userId : userIds) {
            List<WeeklyScore> userScores = grouped.getOrDefault(userId, Collections.emptyList());
            // 按周汇总
            Map<LocalDate, Integer> weeklyTotals = userScores.stream()
                    .collect(Collectors.groupingBy(WeeklyScore::getWeekStartDate,
                            Collectors.summingInt(WeeklyScore::getPoints)));
            double avg = weeklyTotals.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            result.put(userId, avg);
        }
        return result;
    }

    /**
     * 将用户分批（每批GROUP_SIZE人）
     */
    private List<List<User>> partitionUsers(List<User> users) {
        List<List<User>> groups = new ArrayList<>();
        for (int i = 0; i < users.size(); i += GROUP_SIZE) {
            groups.add(users.subList(i, Math.min(i + GROUP_SIZE, users.size())));
        }
        return groups;
    }

    /**
     * 创建小组并添加成员
     */
    private void createGroup(LeagueTier tier, List<User> users, LocalDate weekStart) {
        LeagueGroup group = new LeagueGroup();
        group.setTierId(tier.getId());
        group.setWeekStartDate(weekStart);
        group.setStatus(0);
        leagueGroupMapper.insert(group);

        for (User user : users) {
            LeagueMember member = new LeagueMember();
            member.setGroupId(group.getId());
            member.setUserId(user.getId());
            member.setWeeklyPoints(0);
            member.setRanking(0);
            leagueMemberMapper.insert(member);
        }

        log.info("段位 {} 创建小组 {}，成员 {} 人", tier.getName(), group.getId(), users.size());
    }

    /**
     * 检查用户本周是否有小组
     */
    public boolean hasGroupThisWeek(Long userId) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return leagueMemberMapper.selectCount(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getUserId, userId)
                        .apply("group_id IN (SELECT id FROM t_league_group WHERE week_start_date = {0})", weekStart)) > 0;
    }

    /**
     * 获取用户当前周的小组成员信息
     */
    public LeagueMember getCurrentWeekMember(Long userId) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        return leagueMemberMapper.selectOne(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getUserId, userId)
                        .apply("group_id IN (SELECT id FROM t_league_group WHERE week_start_date = {0})", weekStart));
    }

    /**
     * 获取小组的晋级/降级名额
     */
    public GroupQuota getGroupQuota(int memberCount) {
        if (memberCount < NO_DEMOTE_SIZE) {
            // 不足15人，只晋级不降级
            return new GroupQuota(Math.max(1, (int) Math.ceil(memberCount * PROMOTE_RATIO)), 0);
        }
        int promoteCount = (int) Math.ceil(memberCount * PROMOTE_RATIO);
        int demoteCount = (int) Math.floor(memberCount * DEMOTE_RATIO);
        return new GroupQuota(promoteCount, demoteCount);
    }

    public record GroupQuota(int promoteCount, int demoteCount) {}
}
