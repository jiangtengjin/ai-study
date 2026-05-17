package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionVO {

    private Long id;
    private Integer index;
    private String questionType;
    private String difficulty;
    private String questionContent;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
}
