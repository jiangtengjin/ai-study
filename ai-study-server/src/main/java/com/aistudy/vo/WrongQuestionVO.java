package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WrongQuestionVO {

    private Long questionId;
    private Integer questionIndex;
    private String questionContent;
    private String correctAnswer;
    private String userAnswer;
    private String explanation;
}
