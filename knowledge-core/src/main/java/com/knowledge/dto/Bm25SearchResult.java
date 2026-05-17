package com.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Bm25SearchResult {
    private Long id;
    private Long documentId;
    private String content;
    private Integer chunkIndex;
    private String vectorId;
    private Double score;
}
