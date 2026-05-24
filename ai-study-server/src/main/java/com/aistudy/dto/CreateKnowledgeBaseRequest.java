package com.aistudy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 50, message = "知识库名称不能超过 50 个字符")
    private String name;

    @Size(max = 200, message = "知识库描述不能超过 200 个字符")
    private String description;
}
