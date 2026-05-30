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
public class LeagueSettlementService {

    private final LeagueGroupMapper leagueGroupMapper;
    private final LeagueMemberMapper leagueMemberMapper;
    private final LeagueTierMapper leagueTierMapper;
    private final UserMapper userMapper;
    private final TierService tierService;
    private final LeagueGroupService leagueGroupService;

    /**
     * 执行周联赛结算
     */
    @Transactional
    public void executeSettlement() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        log.info("开始执行周联赛结算，周起始日期: {}", weekStart);

        List<LeagueGroup> groups = leagueGroupMapper.selectList(
                new LambdaQueryWrapper<LeagueGroup>()
                        .eq(LeagueGroup::getWeekStartDate, weekStart)
                        .eq(LeagueGroup::getStatus, 0));

        for (LeagueGroup group : groups) {
            settleGroup(group);
        }

        log.info("周联赛结算完成，处理 {} 个小组", groups.size());
    }

    /**
     * 结算单个小组
     */
    private void settleGroup(LeagueGroup group) {
        List<LeagueMember> members = leagueMemberMapper.selectList(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getGroupId, group.getId())
                        .orderByDesc(LeagueMember::getWeeklyPoints));

        int memberCount = members.size();
        LeagueGroupService.GroupQuota quota = leagueGroupService.getGroupQuota(memberCount);

        LeagueTier groupTier = leagueTierMapper.selectById(group.getTierId());
        LeagueTier maxTier = tierService.getMaxTier();
        LeagueTier minTier = tierService.getMinTier();

        boolean isMaxTier = groupTier != null && maxTier != null && groupTier.getId().equals(maxTier.getId());
        boolean isMinTier = groupTier != null && minTier != null && groupTier.getId().equals(minTier.getId());

        for (int i = 0; i < members.size(); i++) {
            LeagueMember member = members.get(i);
            member.setRanking(i + 1);

            // 周积分0 → 直接降级
            if (member.getWeeklyPoints() == 0 && !isMinTier) {
                member.setResult("demote");
                applyDemote(member.getUserId(), groupTier);
                leagueMemberMapper.updateById(member);
                continue;
            }

            int rank = i + 1;
            if (rank <= quota.promoteCount() && !isMaxTier) {
                member.setResult("promote");
                applyPromote(member.getUserId(), groupTier);
            } else if (rank > memberCount - quota.demoteCount() && !isMinTier) {
                member.setResult("demote");
                applyDemote(member.getUserId(), groupTier);
            } else {
                member.setResult("keep");
            }

            leagueMemberMapper.updateById(member);
        }

        group.setStatus(1);
        leagueGroupMapper.updateById(group);
    }

    /**
     * 执行晋级：更新用户段位 tierId
     */
    private void applyPromote(Long userId, LeagueTier currentTier) {
        LeagueTier nextTier = tierService.getNextTier(currentTier);
        if (nextTier == null) return;

        tierService.updateUserTier(userId, nextTier.getId());
        log.info("用户 {} 晋级: {} → {}", userId, currentTier.getName(), nextTier.getName());
    }

    /**
     * 执行降级：更新用户段位 tierId
     */
    private void applyDemote(Long userId, LeagueTier currentTier) {
        LeagueTier prevTier = tierService.getPreviousTier(currentTier);
        if (prevTier == null) return;

        tierService.updateUserTier(userId, prevTier.getId());
        log.info("用户 {} 降级: {} → {}", userId, currentTier.getName(), prevTier.getName());
    }

    /**
     * 获取用户的历史联赛记录（批量查询，避免 N+1）
     */
    public List<LeagueHistoryVO> getHistory(Long userId, int weeks) {
        LocalDate fromDate = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(weeks);

        List<LeagueMember> records = leagueMemberMapper.selectList(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getUserId, userId)
                        .apply("group_id IN (SELECT id FROM t_league_group WHERE week_start_date >= {0})", fromDate)
                        .orderByDesc(LeagueMember::getCreatedAt));

        if (records.isEmpty()) {
            return List.of();
        }

        // 批量查询关联的 group
        Set<Long> groupIds = records.stream()
                .map(LeagueMember::getGroupId)
                .collect(Collectors.toSet());
        Map<Long, LeagueGroup> groupMap = leagueGroupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(LeagueGroup::getId, g -> g));

        // 批量查询关联的 tier
        Set<Long> tierIds = groupMap.values().stream()
                .map(LeagueGroup::getTierId)
                .collect(Collectors.toSet());
        Map<Long, LeagueTier> tierMap = tierIds.isEmpty()
                ? Map.of()
                : leagueTierMapper.selectBatchIds(tierIds).stream()
                        .collect(Collectors.toMap(LeagueTier::getId, t -> t));

        return records.stream().map(record -> {
            LeagueGroup group = groupMap.get(record.getGroupId());
            LeagueTier tier = group != null ? tierMap.get(group.getTierId()) : null;
            return new LeagueHistoryVO(
                    group != null ? group.getWeekStartDate() : null,
                    tier != null ? tier.getName() : "",
                    tier != null ? tier.getIcon() : "",
                    record.getRanking(),
                    record.getWeeklyPoints(),
                    record.getResult()
            );
        }).toList();
    }

    public record LeagueHistoryVO(
            LocalDate weekStartDate,
            String tierName,
            String tierIcon,
            Integer ranking,
            Integer weeklyPoints,
            String result
    ) {}
}
