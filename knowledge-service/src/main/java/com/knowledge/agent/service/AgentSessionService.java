package com.knowledge.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.agent.entity.AgentMessage;
import com.knowledge.agent.entity.AgentSession;
import com.knowledge.agent.mapper.AgentMessageMapper;
import com.knowledge.agent.mapper.AgentSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentSessionService {

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;

    public AgentSession createSession(String title) {
        AgentSession session = new AgentSession();
        session.setTitle(title != null ? title : "Agent对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public List<AgentSession> listSessions() {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AgentSession>()
                        .orderByDesc(AgentSession::getUpdateTime));
    }

    public List<AgentMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByAsc(AgentMessage::getCreateTime));
    }

    public void deleteSession(Long sessionId) {
        messageMapper.delete(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    public void saveMessage(AgentMessage msg) {
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    public void updateSessionTime(Long sessionId) {
        AgentSession session = new AgentSession();
        session.setId(sessionId);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }
}
