package com.knowledge.agent.dto;

import lombok.Data;

@Data
public class AgentChatRequest {
    private Long sessionId;
    private String question;
}
