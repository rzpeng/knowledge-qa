package com.knowledge.agent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentSessionDTO {
    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
