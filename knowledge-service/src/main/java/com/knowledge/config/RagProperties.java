package com.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    // === Legacy chunking ===
    private int chunkSize = 500;
    private int chunkOverlap = 50;

    // === Retrieval ===
    private int topK = 5;

    // === Hybrid Retrieval ===
    private boolean hybridSearchEnabled = true;
    private int bm25TopK = 10;
    private int vectorTopK = 10;
    private double rrfK = 60;       // RRF constant

    // === Re-ranking ===
    private boolean rerankEnabled = true;
    private int rerankCandidateCount = 15;

    // === Query Rewrite ===
    private boolean queryRewriteEnabled = true;

    // === Parent-Child Chunking ===
    private boolean parentChildEnabled = true;
    private int childChunkSize = 200;
    private int parentChunkSize = 500;
}
