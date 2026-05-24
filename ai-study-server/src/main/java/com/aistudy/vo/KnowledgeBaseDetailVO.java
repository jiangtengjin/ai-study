package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KnowledgeBaseDetailVO {

    private Long id;

    private String name;

    private String description;

    private Integer docCount;

    private LocalDateTime createdAt;

    private List<DocumentVO> documents;
}
