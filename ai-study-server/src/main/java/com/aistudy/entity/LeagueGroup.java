package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_league_group")
public class LeagueGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tierId;

    private LocalDate weekStartDate;

    /** 0-进行中 1-已结算 */
    private Integer status;

    private LocalDateTime createdAt;
}
