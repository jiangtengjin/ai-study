package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionDetailVO {

    private Long questionId;
    private Integer questionIndex;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer;
    private String userAnswer;
    private Integer isCorrect;
    private String explanation;
    private String knowledgePoint;
    private Integer answerTimeSeconds;
}
