package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_league_member")
public class LeagueMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long userId;

    private Integer weeklyPoints;

    private Integer ranking;

    /** 结算结果: promote/keep/demote */
    private String result;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
