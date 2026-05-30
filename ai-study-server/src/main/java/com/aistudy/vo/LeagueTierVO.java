package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeagueTierVO {

    /** 当前段位名称 */
    private String tierName;

    /** 当前段位图标 */
    private String tierIcon;

    /** 当前段位排序 */
    private Integer tierSortOrder;

    /** 累计总积分 */
    private Long totalPoints;

    /** 下一等级名称 */
    private String nextTierName;

    /** 下一等级图标 */
    private String nextTierIcon;

    /** 下一等级所需积分 */
    private Long nextTierPoints;

    /** 距下一等级的积分差 */
    private Long pointsToNext;
}
