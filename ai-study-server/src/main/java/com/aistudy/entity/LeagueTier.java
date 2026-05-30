package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_league_tier")
public class LeagueTier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String icon;

    private Long minPoints;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
