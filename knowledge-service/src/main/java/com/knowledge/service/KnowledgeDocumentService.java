package com.knowledge.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.security.LoginUserDetails;
import com.knowledge.dto.DocumentDTO;
import com.knowledge.config.FileProperties;
import com.knowledge.entity.KnowledgeChunk;
import com.knowledge.entity.KnowledgeDocument;
import com.knowledge.enums.DocumentStatus;
import com.knowledge.enums.FileType;
import com.knowledge.mapper.KnowledgeChunkMapper;
import com.knowledge.mapper.KnowledgeDocumentMapper;
import com.knowledge.rag.MilvusVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final MilvusVectorStore vectorStore;
    private final DocumentProcessService documentProcessService;
    private final FileProperties fileProperties;

    public List<KnowledgeDocument> listAll() {
        return documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .orderByDesc(KnowledgeDocument::getCreateTime)
        );
    }

    public KnowledgeDocument getById(Long id) {
        return documentMapper.selectById(id);
    }

    public KnowledgeDocument upload(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !FileType.isSupported(originalFilename)) {
            throw new IllegalArgumentException("不支持的文件类型，支持: txt, md, pdf, docx");
        }

        String uploadDir = fileProperties.getUploadPath();
        File uploadDirFile = new File(uploadDir).getAbsoluteFile();
        FileUtil.mkdir(uploadDirFile);

        String storedName = IdUtil.fastSimpleUUID() + "_" + originalFilename;
        File storedFile = new File(uploadDirFile, storedName);
        file.transferTo(storedFile.getAbsoluteFile());

        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(originalFilename.substring(0, originalFilename.lastIndexOf(".")));
        document.setFileName(originalFilename);
        document.setFileType(FileType.fromExtension(originalFilename.substring(originalFilename.lastIndexOf(".") + 1)).getExtension());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.PROCESSING.getCode());
        document.setChunkCount(0);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUserDetails loginUser) {
            document.setCreateBy(loginUser.getUsername());
        }
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.insert(document);

        documentProcessService.processDocumentAsync(document.getId(), storedFile);

        return document;
    }

    public KnowledgeDocument update(Long id, DocumentDTO documentDTO) {
        KnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }

        document.setTitle(documentDTO.getTitle());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);

        return document;
    }

    public void delete(Long id) {
        KnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            return;
        }

        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, id));

        vectorStore.deleteByDocumentId(id);

        documentMapper.deleteById(id);
    }
}
