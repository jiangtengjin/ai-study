package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_quiz_session")
public class QuizSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String knowledgeContent;

    private String knowledgeTitle;

    private Integer questionCount;

    private Integer correctCount;

    private Integer score;

    private Integer durationSeconds;

    private String difficulty;

    /** 0-进行中 1-已完成 2-中途退出 */
    private Integer status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
