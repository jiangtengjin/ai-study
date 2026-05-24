package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_quiz_answer")
public class QuizAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long questionId;

    private Long userId;

    private String userAnswer;

    /** 0-错误 1-正确 */
    private Integer isCorrect;

    private Integer answerTimeSeconds;

    private LocalDateTime createdAt;
}
