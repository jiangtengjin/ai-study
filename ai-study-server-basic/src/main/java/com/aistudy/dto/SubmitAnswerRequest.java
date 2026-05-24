package com.aistudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotBlank(message = "答案不能为空")
    private String userAnswer;

    private Integer answerTimeSeconds = 0;
}
