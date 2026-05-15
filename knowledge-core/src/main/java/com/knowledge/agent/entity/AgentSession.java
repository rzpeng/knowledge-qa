package com.knowledge.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
