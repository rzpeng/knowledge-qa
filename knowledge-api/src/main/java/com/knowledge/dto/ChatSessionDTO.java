package com.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionDTO {

    private Long id;

    private String title;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
