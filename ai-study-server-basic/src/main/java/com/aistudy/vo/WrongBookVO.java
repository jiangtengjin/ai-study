package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WrongBookVO {

    private Long answerId;

    private Long sessionId;

    private String sessionTitle;

    private Long questionId;

    private Integer questionIndex;

    private String questionContent;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctAnswer;

    private String userAnswer;

    private String explanation;

    private String knowledgePoint;

    private LocalDateTime createdAt;
}
