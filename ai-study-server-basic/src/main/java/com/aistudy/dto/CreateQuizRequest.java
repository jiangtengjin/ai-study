package com.aistudy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateQuizRequest {

    @NotBlank(message = "知识内容不能为空")
    @Size(min = 10, max = 10000, message = "知识内容长度需在 10-10000 字之间")
    private String content;

    @Min(value = 3, message = "最少 3 题")
    @Max(value = 20, message = "最多 20 题")
    private int questionCount = 10;

    private String difficulty = "balanced";
}
