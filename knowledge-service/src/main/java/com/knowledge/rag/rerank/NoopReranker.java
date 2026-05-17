package com.knowledge.rag.rerank;

import com.knowledge.rag.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default re-ranker that passes results through unchanged.
 * Used when no other Reranker bean is configured (e.g., before LLMReranker is added).
 */
@Component
@ConditionalOnMissingBean(Reranker.class)
public class NoopReranker implements Reranker {

    @Override
    public List<MilvusVectorStore.SearchResult> rerank(
            String query, List<MilvusVectorStore.SearchResult> candidates, int topK) {
        return candidates;
    }
}
