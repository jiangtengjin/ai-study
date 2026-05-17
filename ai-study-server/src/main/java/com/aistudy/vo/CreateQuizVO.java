package com.aistudy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateQuizVO {

    private Long sessionId;
    private String title;
    private Integer questionCount;
    private String status;
}
