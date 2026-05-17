package com.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.config.RagProperties;
import com.knowledge.entity.ChatMessage;
import com.knowledge.entity.ChatSession;
import com.knowledge.entity.KnowledgeChunk;
import com.knowledge.enums.MessageRole;
import com.knowledge.mapper.ChatMessageMapper;
import com.knowledge.mapper.ChatSessionMapper;
import com.knowledge.mapper.KnowledgeChunkMapper;
import com.knowledge.rag.HybridRetriever;
import com.knowledge.rag.MilvusVectorStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个智能知识库助手。请根据提供的知识库内容回答用户问题。
            如果知识库中没有相关信息，请明确告知用户，并尝试基于常识给出有用的回答。
            回答时请引用相关来源。
            """;

    private static final String CONTEXT_TEMPLATE = """
            以下是知识库中的相关内容：

            %s

            ---

            请基于以上内容回答用户问题：%s
            """;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    @Autowired
    private MilvusVectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    private final RagProperties ragProperties;
    private final StringRedisTemplate redisTemplate;
    private final HybridRetriever hybridRetriever;
    private final KnowledgeChunkMapper chunkMapper;

    public List<ChatSession> listSessions() {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    public ChatSession createSession(String title) {
        ChatSession session = new ChatSession();
        session.setTitle(title);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );
    }

    public String chat(Long sessionId, String question) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        saveMessage(sessionId, MessageRole.USER, question);

        List<MilvusVectorStore.SearchResult> searchResults = searchKnowledge(question);

        // Enrich with parent content when parent-child chunking is enabled
        String context;
        if (ragProperties.isParentChildEnabled()) {
            context = searchResults.stream()
                    .map(r -> {
                        String parentContent = lookupParentContent(r);
                        if (parentContent != null) {
                            return "- " + parentContent;
                        }
                        return "- " + r.content();
                    })
                    .distinct()
                    .collect(Collectors.joining("\n\n"));
        } else {
            context = searchResults.stream()
                    .map(r -> "- " + r.content())
                    .collect(Collectors.joining("\n\n"));
        }

        String prompt = String.format(CONTEXT_TEMPLATE, context, question);

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));

        List<ChatMessage> history = getSessionMessages(sessionId);
        for (ChatMessage msg : history) {
            if (msg.getRole().equals(MessageRole.USER.getCode())) {
                messages.add(UserMessage.from(msg.getContent()));
            } else if (msg.getRole().equals(MessageRole.ASSISTANT.getCode())) {
                messages.add(AiMessage.from(msg.getContent()));
            }
        }

        messages.add(UserMessage.from(prompt));

        String answer = chatModel.generate(messages).content().text();

        saveMessage(sessionId, MessageRole.ASSISTANT, answer);

        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        return answer;
    }

    private List<MilvusVectorStore.SearchResult> searchKnowledge(String question) {
        if (ragProperties.isHybridSearchEnabled()) {
            return hybridRetriever.hybridSearch(question);
        }
        // Fallback to pure vector search
        Response<dev.langchain4j.data.embedding.Embedding> response = embeddingModel.embed(question);
        float[] vector = response.content().vector();
        return vectorStore.search(vector, ragProperties.getTopK());
    }

    /**
     * Look up parent content from MySQL when parent-child chunking is enabled.
     * Returns the parent content if found, null otherwise.
     */
    private String lookupParentContent(MilvusVectorStore.SearchResult result) {
        if (result.vectorId() == null) return null;
        try {
            KnowledgeChunk chunk = chunkMapper.selectByVectorId(String.valueOf(result.vectorId()));
            if (chunk != null && chunk.getParentContent() != null && !chunk.getParentContent().isBlank()) {
                return chunk.getParentContent();
            }
        } catch (Exception e) {
            log.debug("Failed to look up parent content for vectorId={}: {}", result.vectorId(), e.getMessage());
        }
        return null;
    }

    private void saveMessage(Long sessionId, MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role.getCode());
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    public void deleteSession(Long sessionId) {
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }
}
