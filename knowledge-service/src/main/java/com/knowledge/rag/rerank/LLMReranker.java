package com.knowledge.rag.rerank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.rag.MilvusVectorStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LLM-based re-ranker that uses DeepSeek to score chunk relevance.
 * Activated when rag.reranker-type=llm (default).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "rag", name = "reranker-type", havingValue = "llm", matchIfMissing = true)
public class LLMReranker implements Reranker {

    private static final String RERANK_PROMPT = """
            你是一个文档相关性评估专家。请评估以下候选文档片段与用户问题的相关性。
            为每个片段给出 1-5 分的相关性评分（5=非常相关，1=不相关）。
            只返回 JSON 数组，格式：[{"index": 0, "score": 5}, {"index": 1, "score": 3}, ...]

            用户问题：%s

            候选文档片段：
            %s
            """;

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    public LLMReranker(@Qualifier("deepseekChatModel") ChatLanguageModel chatModel,
                       ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MilvusVectorStore.SearchResult> rerank(
            String query,
            List<MilvusVectorStore.SearchResult> candidates,
            int topK) {

        if (candidates.isEmpty() || candidates.size() <= topK) {
            return candidates;
        }

        // Build chunk listing (truncate long content to avoid token overflow)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            String content = candidates.get(i).content();
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            sb.append("[").append(i).append("] ").append(content).append("\n\n");
        }

        String prompt = String.format(RERANK_PROMPT, query, sb.toString());

        try {
            String response = chatModel.generate(prompt);
            Map<Integer, Double> scores = parseScores(response, candidates.size());

            // Sort by score descending, take topK
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(
                                    (MilvusVectorStore.SearchResult r) ->
                                            scores.getOrDefault(candidates.indexOf(r), 1.0))
                            .reversed())
                    .limit(topK)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("LLM reranking failed, using original order: {}", e.getMessage());
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }
    }

    private Map<Integer, Double> parseScores(String response, int expectedSize) {
        try {
            // Extract JSON array from response (it might be wrapped in markdown code block)
            String json = response;
            if (json.contains("```")) {
                json = json.replaceAll("```[a-zA-Z]*", "").trim();
            }
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            List<Map<String, Object>> scoreList = objectMapper.readValue(
                    json, new TypeReference<List<Map<String, Object>>>() {});

            Map<Integer, Double> scores = new HashMap<>();
            for (Map<String, Object> entry : scoreList) {
                int idx = ((Number) entry.get("index")).intValue();
                double score = ((Number) entry.get("score")).doubleValue();
                scores.put(idx, score);
            }
            return scores;
        } catch (Exception e) {
            log.warn("Failed to parse reranker scores from: {}", response, e);
            Map<Integer, Double> defaultScores = new HashMap<>();
            for (int i = 0; i < expectedSize; i++) {
                defaultScores.put(i, 1.0);
            }
            return defaultScores;
        }
    }
}
