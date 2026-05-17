package com.knowledge.rag;

import com.knowledge.config.RagProperties;
import com.knowledge.dto.Bm25SearchResult;
import com.knowledge.mapper.KnowledgeChunkMapper;
import com.knowledge.rag.rerank.Reranker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetriever {

    private final MilvusVectorStore vectorStore;
    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;
    private final Reranker reranker;

    private final QueryRewriter queryRewriter;

    /**
     * Hybrid search: query rewrite → vector search + BM25 search → RRF fusion → re-ranking.
     */
    public List<MilvusVectorStore.SearchResult> hybridSearch(String query) {
        List<MilvusVectorStore.SearchResult> vectorResults;
        List<Bm25SearchResult> bm25Results;

        if (!ragProperties.isHybridSearchEnabled()) {
            Response<Embedding> response = embeddingModel.embed(query);
            float[] queryVector = response.content().vector();
            return vectorStore.search(queryVector, ragProperties.getTopK());
        }

        // 0. Query rewriting for better retrieval
        String searchQuery = queryRewriter.rewrite(query);

        // 1. Vector search (use rewritten query for semantic embedding)
        Response<Embedding> response = embeddingModel.embed(searchQuery);
        float[] queryVector = response.content().vector();
        vectorResults = vectorStore.search(queryVector, ragProperties.getVectorTopK());

        // 2. BM25 search (use original query for keyword matching)
        bm25Results = chunkMapper.searchByBm25(query, ragProperties.getBm25TopK());

        log.debug("Vector search returned {} results, BM25 returned {} results (query: '{}')",
                vectorResults.size(), bm25Results.size(), searchQuery);

        // 3. RRF fusion
        List<MilvusVectorStore.SearchResult> merged = rrfMerge(vectorResults, bm25Results, ragProperties.getTopK() * 2);

        // 4. Re-ranking (optional)
        if (ragProperties.isRerankEnabled() && merged.size() > ragProperties.getTopK()) {
            try {
                merged = reranker.rerank(query, merged, ragProperties.getRerankCandidateCount());
            } catch (Exception e) {
                log.warn("Re-ranking failed, using fused results directly: {}", e.getMessage());
            }
        }

        // 5. Take final topK
        return merged.stream().limit(ragProperties.getTopK()).collect(Collectors.toList());
    }

    /**
     * Reciprocal Rank Fusion: combines ranked lists from two sources.
     * RRF score = sum(1 / (k + rank)) for each item across all rankings.
     */
    private List<MilvusVectorStore.SearchResult> rrfMerge(
            List<MilvusVectorStore.SearchResult> vectorResults,
            List<Bm25SearchResult> bm25Results,
            int topK) {

        double k = ragProperties.getRrfK();
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, MilvusVectorStore.SearchResult> resultMap = new HashMap<>();

        // Score vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            MilvusVectorStore.SearchResult r = vectorResults.get(i);
            String key = r.content();
            rrfScores.merge(key, 1.0 / (k + i + 1), Double::sum);
            resultMap.put(key, r);
        }

        // Score BM25 results
        for (int i = 0; i < bm25Results.size(); i++) {
            Bm25SearchResult r = bm25Results.get(i);
            String key = r.getContent();
            rrfScores.merge(key, 1.0 / (k + i + 1), Double::sum);
            if (!resultMap.containsKey(key)) {
                resultMap.put(key, new MilvusVectorStore.SearchResult(
                        r.getContent(), 0.0, r.getDocumentId(), null));
            }
        }

        // Sort by RRF score descending, take topK
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> resultMap.get(entry.getKey()))
                .collect(Collectors.toList());
    }
}
