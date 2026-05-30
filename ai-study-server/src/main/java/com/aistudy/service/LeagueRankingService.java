package com.aistudy.service;

import com.aistudy.entity.*;
import com.aistudy.mapper.LeagueGroupMapper;
import com.aistudy.mapper.LeagueMemberMapper;
import com.aistudy.mapper.LeagueTierMapper;
import com.aistudy.mapper.UserMapper;
import com.aistudy.vo.LeagueRankingVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeagueRankingService {

    private final LeagueMemberMapper leagueMemberMapper;
    private final LeagueGroupMapper leagueGroupMapper;
    private final LeagueTierMapper leagueTierMapper;
    private final UserMapper userMapper;
    private final LeagueGroupService leagueGroupService;

    /**
     * 获取用户当前周联赛排名
     */
    public LeagueRankingVO getRanking(Long userId) {
        LeagueMember myMember = leagueGroupService.getCurrentWeekMember(userId);
        if (myMember == null) {
            return LeagueRankingVO.builder()
                    .rankings(List.of())
                    .myRanking(0)
                    .myPoints(0)
                    .pointsToPromote(0)
                    .build();
        }

        LeagueGroup group = leagueGroupMapper.selectById(myMember.getGroupId());
        LeagueTier tier = leagueTierMapper.selectById(group.getTierId());

        // 获取小组所有成员，按周积分排序
        List<LeagueMember> members = leagueMemberMapper.selectList(
                new LambdaQueryWrapper<LeagueMember>()
                        .eq(LeagueMember::getGroupId, group.getId())
                        .orderByDesc(LeagueMember::getWeeklyPoints));

        // 批量获取用户信息
        List<Long> userIds = members.stream().map(LeagueMember::getUserId).toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 构建排名列表
        List<LeagueRankingVO.RankingItem> rankings = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            LeagueMember m = members.get(i);
            User user = userMap.get(m.getUserId());
            rankings.add(LeagueRankingVO.RankingItem.builder()
                    .ranking(i + 1)
                    .userId(m.getUserId())
                    .nickname(user != null ? user.getNickname() : "未知用户")
                    .avatar(user != null ? user.getAvatar() : null)
                    .weeklyPoints(m.getWeeklyPoints())
                    .build());
        }

        // 计算晋级线积分
        LeagueGroupService.GroupQuota quota = leagueGroupService.getGroupQuota(members.size());
        int pointsToPromote = 0;
        if (quota.promoteCount() > 0 && members.size() >= quota.promoteCount()) {
            pointsToPromote = members.get(quota.promoteCount() - 1).getWeeklyPoints();
        }

        // 找到当前用户排名
        int myRanking = 0;
        int myPoints = myMember.getWeeklyPoints();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getUserId().equals(userId)) {
                myRanking = i + 1;
                break;
            }
        }

        return LeagueRankingVO.builder()
                .groupId(group.getId())
                .tierName(tier != null ? tier.getName() : "")
                .tierIcon(tier != null ? tier.getIcon() : "")
                .rankings(rankings)
                .myRanking(myRanking)
                .myPoints(myPoints)
                .pointsToPromote(Math.max(0, pointsToPromote - myPoints))
                .promoteCount(quota.promoteCount())
                .demoteCount(quota.demoteCount())
                .build();
    }
}
