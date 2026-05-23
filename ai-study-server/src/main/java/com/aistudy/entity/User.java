package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nickname;

    private String avatar;

    private String email;

    private String password;

    /** 认证类型: email/github/wechat */
    private String authType;

    /** 第三方平台用户ID */
    private String authId;

    private Integer vipLevel;

    private LocalDateTime vipExpireTime;

    private Integer totalQuizzes;

    private Integer totalCorrect;

    private Integer totalQuestions;

    private Integer streakDays;

    private LocalDate lastStudyDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
