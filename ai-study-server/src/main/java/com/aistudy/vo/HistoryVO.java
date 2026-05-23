package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoryVO {

    private Long sessionId;

    private String title;

    private Integer questionCount;

    private Integer correctCount;

    private Integer score;

    private Integer durationSeconds;

    private String difficulty;

    private LocalDateTime createdAt;
}
