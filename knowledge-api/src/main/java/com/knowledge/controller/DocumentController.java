package com.knowledge.controller;

import cn.hutool.core.bean.BeanUtil;
import com.knowledge.dto.DocumentDTO;
import com.knowledge.entity.KnowledgeDocument;
import com.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final KnowledgeDocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        KnowledgeDocument document = documentService.upload(file);
        return ResponseEntity.ok(toDTO(document));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> list() {
        List<KnowledgeDocument> documents = documentService.listAll();
        List<DocumentDTO> dtos = documents.stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> get(@PathVariable Long id) {
        KnowledgeDocument document = documentService.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(document));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> update(@PathVariable Long id, @RequestBody DocumentDTO documentDTO) {
        KnowledgeDocument document = documentService.update(id, documentDTO);
        return ResponseEntity.ok(toDTO(document));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private DocumentDTO toDTO(KnowledgeDocument document) {
        DocumentDTO dto = new DocumentDTO();
        BeanUtil.copyProperties(document, dto);
        return dto;
    }
}
