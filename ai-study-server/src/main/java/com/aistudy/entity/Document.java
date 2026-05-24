package com.aistudy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private String fileName;

    private Long fileSize;

    private String fileType;

    /** processing / completed / failed */
    private String status;

    private LocalDateTime createdAt;
}
