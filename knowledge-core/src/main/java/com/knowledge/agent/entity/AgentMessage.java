package com.knowledge.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;          // USER / ASSISTANT / TOOL

    private String content;       // text content

    private String toolName;      // tool name for TOOL role

    private String toolArgs;      // JSON arguments for ASSISTANT with tool calls

    private String toolResult;    // JSON result for TOOL role

    private LocalDateTime createTime;
}
