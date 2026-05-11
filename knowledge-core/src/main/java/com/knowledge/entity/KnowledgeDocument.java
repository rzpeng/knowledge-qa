package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    private LocalDateTime createTime;

    private Long deptId;

    private Long regionId;

    private String createBy;

    private LocalDateTime updateTime;
}
