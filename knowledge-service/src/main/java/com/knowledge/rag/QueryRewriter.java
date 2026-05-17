package com.knowledge.rag;

import com.knowledge.config.RagProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryRewriter {

    private static final String REWRITE_PROMPT = """
            你是一个检索查询改写专家。将用户的问题改写成更适合知识库检索的形式。

            要求：
            - 提取核心关键词和实体
            - 补充同义词和相关术语
            - 保持原始意图
            - 输出简洁的检索查询（一句话）

            用户问题：%s

            检索查询：
            """;

    private final ChatLanguageModel chatModel;
    private final RagProperties ragProperties;

    public QueryRewriter(@Qualifier("deepseekChatModel") ChatLanguageModel chatModel,
                         RagProperties ragProperties) {
        this.chatModel = chatModel;
        this.ragProperties = ragProperties;
    }

    public String rewrite(String originalQuery) {
        if (!ragProperties.isQueryRewriteEnabled()) {
            return originalQuery;
        }

        String prompt = String.format(REWRITE_PROMPT, originalQuery);
        try {
            String rewritten = chatModel.generate(prompt).trim();
            log.debug("Query rewritten: '{}' -> '{}'", originalQuery, rewritten);
            return rewritten;
        } catch (Exception e) {
            log.warn("Query rewriting failed, using original: {}", e.getMessage());
            return originalQuery;
        }
    }
}
