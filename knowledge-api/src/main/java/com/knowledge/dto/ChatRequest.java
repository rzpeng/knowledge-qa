package com.knowledge.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private Long sessionId;

    private String question;

    private boolean stream = false;
}
