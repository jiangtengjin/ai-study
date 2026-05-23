package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileStatsVO {

    private Integer totalQuizzes;

    private Integer totalQuestions;

    private Integer totalCorrect;

    private Double correctRate;

    private Integer streakDays;

    private Integer averageScore;
}
