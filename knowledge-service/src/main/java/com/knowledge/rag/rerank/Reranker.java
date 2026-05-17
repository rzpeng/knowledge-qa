package com.knowledge.rag.rerank;

import com.knowledge.rag.MilvusVectorStore;

import java.util.List;

public interface Reranker {

    /**
     * Re-rank candidate chunks by their relevance to the query.
     *
     * @param query      the original user question
     * @param candidates candidate chunks from hybrid retrieval
     * @param topK       max number of results to return
     * @return re-ranked results (best first)
     */
    List<MilvusVectorStore.SearchResult> rerank(String query,
                                                  List<MilvusVectorStore.SearchResult> candidates,
                                                  int topK);
}
