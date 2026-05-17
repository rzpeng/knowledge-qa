package com.knowledge.service;

import cn.hutool.core.io.FileUtil;
import com.knowledge.config.RagProperties;
import com.knowledge.entity.KnowledgeChunk;
import com.knowledge.entity.KnowledgeDocument;
import com.knowledge.enums.DocumentStatus;
import com.knowledge.enums.FileType;
import com.knowledge.mapper.KnowledgeChunkMapper;
import com.knowledge.mapper.KnowledgeDocumentMapper;
import com.knowledge.rag.MilvusVectorStore;
import com.knowledge.rag.chunking.SemanticChunker;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final DocumentParserService parserService;
    private final MilvusVectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;
    private final SemanticChunker semanticChunker;


    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 3000;
    private static final int BATCH_SIZE = 5;


    @Async
    public void processDocumentAsync(Long documentId, File file) {
        try {
            processDocument(documentId, file);
        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);
            updateDocumentStatus(documentId, DocumentStatus.FAILED, e.getMessage());
        } finally {
            FileUtil.del(file);
        }
    }

    private void processDocument(Long documentId, File file) throws IOException {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        FileType fileType = FileType.fromExtension(document.getFileType());
        String content = parserService.parseDocument(file, fileType);

        List<String> chunks;
        List<String> parentContents;
        List<Integer> parentIndices;

        if (ragProperties.isParentChildEnabled()) {
            // Parent-child chunking: small chunks for retrieval, parent for LLM context
            List<SemanticChunker.ChildChunk> childChunks = semanticChunker.split(content);
            chunks = new ArrayList<>();
            parentContents = new ArrayList<>();
            parentIndices = new ArrayList<>();
            for (SemanticChunker.ChildChunk cc : childChunks) {
                chunks.add(cc.getContent());
                parentContents.add(cc.getParentContent());
                parentIndices.add(cc.getParentIndex());
            }
            log.info("Parent-child chunking: {} child chunks from {} parent chunks (childSize={}, parentSize={})",
                    chunks.size(), parentIndices.stream().distinct().count(),
                    ragProperties.getChildChunkSize(), ragProperties.getParentChunkSize());
        } else {
            // Legacy chunking
            chunks = parserService.splitIntoChunks(
                    content,
                    ragProperties.getChunkSize(),
                    ragProperties.getChunkOverlap()
            );
            parentContents = null;
            parentIndices = null;
        }

        List<Long> vectorIds = new ArrayList<>();

        try {
            List<float[]> vectors = embedWithRetry(chunks);

            vectorIds = vectorStore.insert(chunks, vectors, documentId);

            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(documentId);
                chunk.setContent(chunks.get(i));
                chunk.setChunkIndex(i);
                chunk.setVectorId(String.valueOf(vectorIds.get(i)));
                if (parentContents != null) {
                    chunk.setParentContent(parentContents.get(i));
                    chunk.setParentIndex(parentIndices.get(i));
                }
                chunk.setCreateTime(LocalDateTime.now());
                chunkMapper.insert(chunk);
            }

            document.setStatus(DocumentStatus.SUCCESS.getCode());
            log.info("文档 {} 处理成功，向量化完成", documentId);
        } catch (Exception e) {
            log.error("文档 {} 向量化失败，已重试{}次: {}", documentId, MAX_RETRIES, e.getMessage());
            document.setStatus(DocumentStatus.FAILED.getCode());
            document.setErrorMsg("向量化失败: " + e.getMessage());
            throw new RuntimeException("文档向量化失败", e);
        }

        document.setChunkCount(chunks.size());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private List<float[]> embedWithRetry(List<String> chunks) throws Exception {
        log.info("开始向量化，共{}个文本块", chunks.size());

        int totalBatches = (chunks.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.info("分为{}批处理，每批最多{}个", totalBatches, BATCH_SIZE);

        List<float[]> allVectors = new ArrayList<>();
        int processedChunks = 0;

        for (int batchIdx = 0; batchIdx < totalBatches; batchIdx++) {
            int fromIndex = batchIdx * BATCH_SIZE;
            int toIndex = Math.min(fromIndex + BATCH_SIZE, chunks.size());
            List<String> batchChunks = chunks.subList(fromIndex, toIndex);

            log.info("处理第{}/{}批，包含{}个文本块", batchIdx + 1, totalBatches, batchChunks.size());

            List<float[]> batchVectors = embedBatchWithRetry(batchChunks, batchIdx + 1, totalBatches);
            allVectors.addAll(batchVectors);
            processedChunks += batchChunks.size();

            log.info("累计处理{}/{}个文本块，获得{}个向量",
                    processedChunks, chunks.size(), allVectors.size());
        }

        log.info("向量化完成，共{}个向量", allVectors.size());
        return allVectors;
    }

    private List<float[]> embedBatchWithRetry(List<String> batchChunks, int currentBatch, int totalBatches)
            throws Exception {
        List<float[]> vectors = new ArrayList<>();
        int failedCount = 0;

        for (int i = 0; i < batchChunks.size(); i++) {
            Exception lastException = null;
            long backoffMs = INITIAL_BACKOFF_MS;
            boolean success = false;

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    TextSegment segment = TextSegment.from(batchChunks.get(i));
                    dev.langchain4j.data.embedding.Embedding embedding =
                            embeddingModel.embed(segment).content();
                    vectors.add(embedding.vector());
                    success = true;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("第{}/{}批第{}个文本块向量化失败（第{}/{}次）: {}",
                            currentBatch, totalBatches, i + 1, attempt, MAX_RETRIES, e.getMessage());
                    if (attempt < MAX_RETRIES) {
                        TimeUnit.MILLISECONDS.sleep(backoffMs);
                        backoffMs *= 2;
                    }
                }
            }

            if (!success) {
                failedCount++;
                log.error("第{}/{}批第{}个文本块向量化失败，已跳过", currentBatch, totalBatches, i + 1);
            }
        }

        if (vectors.isEmpty() && !batchChunks.isEmpty()) {
            throw new RuntimeException("第" + currentBatch + "/" + totalBatches + "批所有文本块向量化均失败");
        }
        if (failedCount > 0) {
            log.warn("第{}/{}批向量化完成，{}/{}个成功，{}个失败",
                    currentBatch, totalBatches, vectors.size(), batchChunks.size(), failedCount);
        } else {
            log.info("第{}/{}批向量化成功", currentBatch, totalBatches);
        }
        return vectors;
    }

    private void updateDocumentStatus(Long documentId, DocumentStatus status, String errorMsg) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(documentId);
        document.setStatus(status.getCode());
        document.setErrorMsg(errorMsg);
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
    }
}
