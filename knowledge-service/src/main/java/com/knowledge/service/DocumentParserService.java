package com.knowledge.service;

import cn.hutool.core.io.FileUtil;
import com.knowledge.enums.FileType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentParserService {

    public String parseDocument(File file, FileType fileType) throws IOException {
        return switch (fileType) {
            case TXT, MD -> parseTextFile(file);
            case PDF -> parsePdfFile(file);
            case DOCX -> parseDocxFile(file);
            case DOC -> throw new IOException("暂不支持.doc格式，请转换为.docx格式");
        };
    }

    private String parseTextFile(File file) throws IOException {
        return FileUtil.readString(file, StandardCharsets.UTF_8);
    }

    private String parsePdfFile(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String parseDocxFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    content.append(text).append("\n");
                }
            }
        }
        return content.toString();
    }

    public List<String> splitIntoChunks(String content, int chunkSize, int overlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int contentLength = content.length();

        while (start < contentLength) {
            int end = Math.min(start + chunkSize, contentLength);
            if (end < contentLength) {
                int lastNewLine = content.lastIndexOf('\n', end);
                if (lastNewLine > start) {
                    end = lastNewLine;
                }
            }
            if (end <= start) {
                end = Math.min(start + chunkSize, contentLength);
            }
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
            while (start < contentLength && content.charAt(start) == '\n') {
                start++;
            }
        }

        return chunks;
    }
}
