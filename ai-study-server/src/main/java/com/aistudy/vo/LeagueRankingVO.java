package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LeagueRankingVO {

    /** 小组ID */
    private Long groupId;

    /** 段位名称 */
    private String tierName;

    /** 段位图标 */
    private String tierIcon;

    /** 排名列表 */
    private List<RankingItem> rankings;

    /** 当前用户排名 */
    private Integer myRanking;

    /** 当前用户周积分 */
    private Integer myPoints;

    /** 距晋级线积分差 */
    private Integer pointsToPromote;

    /** 晋级名额 */
    private Integer promoteCount;

    /** 降级名额 */
    private Integer demoteCount;

    @Data
    @Builder
    public static class RankingItem {
        private Integer ranking;
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer weeklyPoints;
    }
}
