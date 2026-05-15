package com.knowledge.agent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentMessageDTO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String toolName;
    private String toolArgs;
    private String toolResult;
    private LocalDateTime createTime;
}
