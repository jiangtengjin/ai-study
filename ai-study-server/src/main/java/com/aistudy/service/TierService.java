package com.aistudy.service;

import com.aistudy.entity.LeagueTier;
import com.aistudy.entity.User;
import com.aistudy.mapper.LeagueTierMapper;
import com.aistudy.mapper.UserMapper;
import com.aistudy.vo.LeagueTierVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TierService {

    private final LeagueTierMapper leagueTierMapper;
    private final UserMapper userMapper;

    /**
     * 获取用户当前段位信息
     */
    public LeagueTierVO getTierInfo(Long userId) {
        User user = userMapper.selectById(userId);
        long totalPoints = (user != null && user.getTotalPoints() != null) ? user.getTotalPoints() : 0L;

        List<LeagueTier> tiers = leagueTierMapper.selectList(
                new LambdaQueryWrapper<LeagueTier>().orderByAsc(LeagueTier::getSortOrder));

        if (tiers.isEmpty()) {
            return LeagueTierVO.builder()
                    .tierName("铜牌").tierIcon("🥉").tierSortOrder(1)
                    .totalPoints(totalPoints).build();
        }

        // 获取当前段位：优先使用 tierId，否则按积分推导并持久化
        LeagueTier currentTier;
        if (user != null && user.getTierId() != null) {
            currentTier = leagueTierMapper.selectById(user.getTierId());
            if (currentTier == null) {
                currentTier = resolveTierByPoints(tiers, totalPoints);
                saveTierId(user, currentTier);
            }
        } else {
            currentTier = resolveTierByPoints(tiers, totalPoints);
            if (user != null) {
                saveTierId(user, currentTier);
            }
        }

        // 找到下一等级
        LeagueTier nextTier = null;
        for (LeagueTier tier : tiers) {
            if (tier.getSortOrder() > currentTier.getSortOrder()) {
                nextTier = tier;
                break;
            }
        }

        LeagueTierVO.LeagueTierVOBuilder builder = LeagueTierVO.builder()
                .tierName(currentTier.getName())
                .tierIcon(currentTier.getIcon())
                .tierSortOrder(currentTier.getSortOrder())
                .totalPoints(totalPoints);

        if (nextTier != null) {
            builder.nextTierName(nextTier.getName())
                    .nextTierIcon(nextTier.getIcon())
                    .nextTierPoints(nextTier.getMinPoints())
                    .pointsToNext(Math.max(0, nextTier.getMinPoints() - totalPoints));
        }

        return builder.build();
    }

    /**
     * 根据累计积分推导段位
     */
    public LeagueTier resolveTierByPoints(List<LeagueTier> tiers, long totalPoints) {
        LeagueTier result = tiers.get(0);
        for (LeagueTier tier : tiers) {
            if (totalPoints >= tier.getMinPoints()) {
                result = tier;
            }
        }
        return result;
    }

    /**
     * 根据累计积分获取段位（无持久化）
     */
    public LeagueTier getTierByPoints(long totalPoints) {
        List<LeagueTier> tiers = leagueTierMapper.selectList(
                new LambdaQueryWrapper<LeagueTier>().orderByAsc(LeagueTier::getSortOrder));
        if (tiers.isEmpty()) return null;
        return resolveTierByPoints(tiers, totalPoints);
    }

    /**
     * 更新用户段位（晋升/降级时调用）
     */
    public void updateUserTier(Long userId, Long tierId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setTierId(tierId);
            userMapper.updateById(user);
        }
    }

    /**
     * 获取指定段位的上一级
     */
    public LeagueTier getPreviousTier(LeagueTier currentTier) {
        return leagueTierMapper.selectOne(
                new LambdaQueryWrapper<LeagueTier>()
                        .eq(LeagueTier::getSortOrder, currentTier.getSortOrder() - 1));
    }

    /**
     * 获取指定段位的下一级
     */
    public LeagueTier getNextTier(LeagueTier currentTier) {
        return leagueTierMapper.selectOne(
                new LambdaQueryWrapper<LeagueTier>()
                        .eq(LeagueTier::getSortOrder, currentTier.getSortOrder() + 1));
    }

    /**
     * 获取最高段位
     */
    public LeagueTier getMaxTier() {
        return leagueTierMapper.selectOne(
                new LambdaQueryWrapper<LeagueTier>()
                        .orderByDesc(LeagueTier::getSortOrder)
                        .last("LIMIT 1"));
    }

    /**
     * 获取最低段位
     */
    public LeagueTier getMinTier() {
        return leagueTierMapper.selectOne(
                new LambdaQueryWrapper<LeagueTier>()
                        .orderByAsc(LeagueTier::getSortOrder)
                        .last("LIMIT 1"));
    }

    private void saveTierId(User user, LeagueTier tier) {
        user.setTierId(tier.getId());
        userMapper.updateById(user);
    }
}
