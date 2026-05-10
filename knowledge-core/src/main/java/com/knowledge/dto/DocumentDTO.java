package com.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentDTO {

    private Long id;

    private String title;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private Integer chunkCount;

    private String errorMsg;

    private LocalDateTime createTime;
}
