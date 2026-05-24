package com.aistudy.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String nickname;

    private String avatar;

    private String email;

    private Integer vipLevel;

    private Integer totalQuizzes;

    private Integer totalCorrect;

    private Integer totalQuestions;

    private Integer streakDays;

    private LocalDate lastStudyDate;

    private LocalDateTime createdAt;
}
