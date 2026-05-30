package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnswerResultVO {

    private Boolean isCorrect;
    private String correctAnswer;
    private String explanation;
    private String knowledgePoint;
    private Integer currentProgress;
    private Integer totalQuestions;
    private Integer streak;
    private Integer points;
}
