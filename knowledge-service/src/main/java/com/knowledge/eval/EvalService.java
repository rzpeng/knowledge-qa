package com.knowledge.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.config.RagProperties;
import com.knowledge.mapper.KnowledgeDocumentMapper;
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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvalService {

    private static final String EVAL_DATASET = "/eval/test-dataset.json";

    private static final String JUDGE_PROMPT = """
            你是一个答案质量评估专家。请评估以下AI回答与标准答案的相关性和准确性。
            请给出1-5分的评分（5=完全正确且相关，1=完全不相关或错误）。
            只返回一个数字分数（1-5）。

            问题：%s

            标准答案：%s

            AI回答：%s

            评分：
            """;

    private static final int EVAL_TOP_K = 5;

    private final HybridRetriever hybridRetriever;
    private final EmbeddingModel embeddingModel;
    private final MilvusVectorStore vectorStore;
    private final RagProperties ragProperties;
    private final ChatLanguageModel chatModel;
    private final KnowledgeDocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    public EvalReport evaluate() {
        List<EvalQuestion> questions = loadQuestions();
        if (questions.isEmpty()) {
            return EvalReport.empty("No test questions found in " + EVAL_DATASET);
        }

        List<EvalReport.RetrievalResult> retrievalResults = new ArrayList<>();

        // Evaluate hybrid search if enabled
        boolean useHybrid = ragProperties.isHybridSearchEnabled();
        boolean useRerank = ragProperties.isRerankEnabled();

        List<EvalReport.AblationResult> ablationResults = new ArrayList<>();

        // 1. Full pipeline (hybrid + rerank)
        EvalReport.PipelineMetrics fullMetrics = evaluateRetrieval(questions, true, true);
        ablationResults.add(new EvalReport.AblationResult("full", fullMetrics));

        // 2. Hybrid only (no rerank)
        EvalReport.PipelineMetrics hybridOnlyMetrics = evaluateRetrieval(questions, true, false);
        ablationResults.add(new EvalReport.AblationResult("hybrid_only", hybridOnlyMetrics));

        // 3. Pure vector search baseline
        EvalReport.PipelineMetrics vectorOnlyMetrics = evaluateRetrieval(questions, false, false);
        ablationResults.add(new EvalReport.AblationResult("vector_only", vectorOnlyMetrics));

        // Answer quality evaluation (on full pipeline)
        List<EvalReport.AnswerScore> answerScores = evaluateAnswers(questions);

        return EvalReport.builder()
                .totalQuestions(questions.size())
                .retrievalResults(retrievalResults)
                .ablationResults(ablationResults)
                .answerScores(answerScores)
                .averageAnswerScore(answerScores.stream()
                        .mapToDouble(EvalReport.AnswerScore::getScore)
                        .average().orElse(0.0))
                .build();
    }

    private EvalReport.PipelineMetrics evaluateRetrieval(
            List<EvalQuestion> questions, boolean useHybrid, boolean useRerank) {

        double totalPrecision = 0;
        double totalRecall = 0;
        double totalMrr = 0;
        int totalRelevantRetrieved = 0;
        int totalRelevantAll = 0;

        // Temporarily override settings
        boolean origHybrid = ragProperties.isHybridSearchEnabled();
        boolean origRerank = ragProperties.isRerankEnabled();
        ragProperties.setHybridSearchEnabled(useHybrid);
        ragProperties.setRerankEnabled(useRerank);

        try {
            for (EvalQuestion q : questions) {
                List<MilvusVectorStore.SearchResult> results = search(q.getQuestion());

                Set<Long> relevantDocIds = new HashSet<>(q.getRelevantDocumentIds());
                Set<Long> retrievedDocIds = results.stream()
                        .map(MilvusVectorStore.SearchResult::documentId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // Precision@K
                long relevantRetrieved = retrievedDocIds.stream()
                        .filter(relevantDocIds::contains)
                        .count();
                double precision = results.isEmpty() ? 0 : (double) relevantRetrieved / results.size();

                // Recall
                double recall = relevantDocIds.isEmpty() ? 0 :
                        (double) relevantRetrieved / relevantDocIds.size();

                // MRR: reciprocal rank of first relevant result
                double mrr = 0;
                for (int i = 0; i < results.size(); i++) {
                    if (relevantDocIds.contains(results.get(i).documentId())) {
                        mrr = 1.0 / (i + 1);
                        break;
                    }
                }

                totalPrecision += precision;
                totalRecall += recall;
                totalMrr += mrr;
                totalRelevantRetrieved += relevantRetrieved;
                totalRelevantAll += relevantDocIds.size();
            }
        } finally {
            // Restore original settings
            ragProperties.setHybridSearchEnabled(origHybrid);
            ragProperties.setRerankEnabled(origRerank);
        }

        int n = questions.size();
        return EvalReport.PipelineMetrics.builder()
                .precisionK(totalPrecision / n)
                .recallK(totalRecall / n)
                .mrr(totalMrr / n)
                .totalRelevantRetrieved(totalRelevantRetrieved)
                .totalRelevantAll(totalRelevantAll)
                .build();
    }

    private List<MilvusVectorStore.SearchResult> search(String question) {
        if (ragProperties.isHybridSearchEnabled()) {
            return hybridRetriever.hybridSearch(question);
        }
        Response<dev.langchain4j.data.embedding.Embedding> resp = embeddingModel.embed(question);
        return vectorStore.search(resp.content().vector(), EVAL_TOP_K);
    }

    private List<EvalReport.AnswerScore> evaluateAnswers(List<EvalQuestion> questions) {
        List<EvalReport.AnswerScore> scores = new ArrayList<>();

        for (EvalQuestion q : questions.subList(0, Math.min(questions.size(), 10))) {
            try {
                List<MilvusVectorStore.SearchResult> results = search(q.getQuestion());
                String context = results.stream()
                        .map(r -> "- " + r.content())
                        .collect(Collectors.joining("\n\n"));

                String prompt = String.format("""
                        以下是知识库中的相关内容：

                        %s

                        ---

                        请基于以上内容回答用户问题：%s
                        """, context, q.getQuestion());

                String answer = chatModel.generate(
                        List.of(SystemMessage.from("你是一个智能知识库助手。"),
                                UserMessage.from(prompt))
                ).content().text();

                String judgePrompt = String.format(JUDGE_PROMPT, q.getQuestion(), q.getExpectedAnswer(), answer);
                String judgeResponse = chatModel.generate(
                        List.of(UserMessage.from(judgePrompt))
                ).content().text();

                double score = parseScore(judgeResponse);
                scores.add(new EvalReport.AnswerScore(q.getId(), q.getQuestion(), score, answer));
            } catch (Exception e) {
                log.warn("Answer evaluation failed for question {}: {}", q.getId(), e.getMessage());
            }
        }

        return scores;
    }

    private double parseScore(String response) {
        try {
            // Extract first number found in response
            String trimmed = response.trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(trimmed);
            if (m.find()) {
                double score = Double.parseDouble(m.group());
                return Math.max(1, Math.min(5, score));
            }
        } catch (Exception e) {
            log.debug("Failed to parse score from: {}", response);
        }
        return 1.0;
    }

    private List<EvalQuestion> loadQuestions() {
        try {
            InputStream is = getClass().getResourceAsStream(EVAL_DATASET);
            if (is == null) {
                log.warn("Test dataset not found: {}", EVAL_DATASET);
                return List.of();
            }
            return objectMapper.readValue(is, new TypeReference<List<EvalQuestion>>() {});
        } catch (Exception e) {
            log.error("Failed to load test dataset: {}", e.getMessage());
            return List.of();
        }
    }
}
