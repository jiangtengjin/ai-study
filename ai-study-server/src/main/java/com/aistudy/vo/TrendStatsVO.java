package com.aistudy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendStatsVO {

    /**
     * 日期（格式：YYYY-MM-DD）
     */
    private String date;

    /**
     * 答题总数
     */
    private Integer totalQuestions;

    /**
     * 正确答题数
     */
    private Integer correctAnswers;

    /**
     * 正确率（百分比，保留两位小数）
     */
    private Double accuracy;

    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;

    /**
     * 获得积分
     */
    private Integer pointsEarned;
}
