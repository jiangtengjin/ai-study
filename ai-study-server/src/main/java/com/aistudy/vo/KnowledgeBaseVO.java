package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseVO {

    private Long id;

    private String name;

    private String description;

    private Integer docCount;

    private LocalDateTime createdAt;
}
