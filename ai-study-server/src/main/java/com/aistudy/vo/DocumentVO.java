package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String status;

    private LocalDateTime createdAt;
}
