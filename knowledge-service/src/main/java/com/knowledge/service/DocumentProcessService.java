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
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        List<String> chunks = parserService.splitIntoChunks(
                content,
                ragProperties.getChunkSize(),
                ragProperties.getChunkOverlap()
        );

        List<Long> vectorIds = new ArrayList<>();
        
        try {
            List<float[]> vectors = new ArrayList<>();
            for (String chunk : chunks) {
                Response<dev.langchain4j.data.embedding.Embedding> response = embeddingModel.embed(chunk);
                float[] vector = response.content().vector();
                vectors.add(vector);
            }

            vectorIds = vectorStore.insert(chunks, vectors, documentId);

            for (int i = 0; i < chunks.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(documentId);
                chunk.setContent(chunks.get(i));
                chunk.setChunkIndex(i);
                chunk.setVectorId(String.valueOf(vectorIds.get(i)));
                chunk.setCreateTime(LocalDateTime.now());
                chunkMapper.insert(chunk);
            }

            document.setStatus(DocumentStatus.SUCCESS.getCode());
            log.info("文档 {} 处理成功，向量化完成", documentId);
        } catch (Exception e) {
            log.warn("文档 {} 向量化失败，但文档已保存: {}", documentId, e.getMessage());
            // 如果向量化失败，仍然标记为成功，但记录错误信息
            document.setStatus(DocumentStatus.SUCCESS.getCode());
            document.setErrorMsg("向量化失败: " + e.getMessage());
        }

        document.setChunkCount(chunks.size());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
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
