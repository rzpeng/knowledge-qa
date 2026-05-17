package com.knowledge.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalReport {

    private int totalQuestions;
    private List<RetrievalResult> retrievalResults;
    private List<AblationResult> ablationResults;
    private List<AnswerScore> answerScores;
    private double averageAnswerScore;

    public static EvalReport empty(String reason) {
        return EvalReport.builder()
                .totalQuestions(0)
                .retrievalResults(List.of())
                .ablationResults(List.of())
                .answerScores(List.of())
                .averageAnswerScore(0.0)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalResult {
        private String questionId;
        private String question;
        private int rank;
        private Long retrievedDocumentId;
        private boolean relevant;
        private double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AblationResult {
        private String name;
        private PipelineMetrics metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineMetrics {
        private double precisionK;
        private double recallK;
        private double mrr;
        private int totalRelevantRetrieved;
        private int totalRelevantAll;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerScore {
        private String questionId;
        private String question;
        private double score;
        private String answer;
    }
}
