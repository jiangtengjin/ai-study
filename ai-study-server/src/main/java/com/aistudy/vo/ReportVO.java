package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportVO {

    private Long sessionId;
    private String title;
    private Integer score;
    private Integer correctCount;
    private Integer questionCount;
    private Integer durationSeconds;
    private String rating;
    private String knowledgeSummary;
    private List<WrongQuestionVO> wrongQuestions;
    private List<String> strengthPoints;
    private List<String> weakPoints;
    private Integer points;
}
