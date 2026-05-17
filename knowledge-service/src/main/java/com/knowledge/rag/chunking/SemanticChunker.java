package com.knowledge.rag.chunking;

import com.knowledge.config.RagProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SemanticChunker {

    private final RagProperties ragProperties;

    /**
     * Split document content into parent-child chunks.
     * Parent chunks (~parentChunkSize chars) provide rich context for LLM,
     * child chunks (~childChunkSize chars) provide precision for retrieval.
     */
    public List<ChildChunk> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        int childSize = ragProperties.getChildChunkSize();
        int parentSize = ragProperties.getParentChunkSize();

        // 1. Split into paragraphs (double newline boundary)
        List<String> paragraphs = extractParagraphs(content);
        if (paragraphs.isEmpty()) {
            return List.of();
        }

        // 2. Group paragraphs into parent chunks
        List<String> parents = buildParentChunks(paragraphs, parentSize);

        // 3. Split each parent into child chunks
        List<ChildChunk> children = new ArrayList<>();
        for (int pi = 0; pi < parents.size(); pi++) {
            List<String> childTexts = splitChildChunks(parents.get(pi), childSize);
            for (String ct : childTexts) {
                children.add(new ChildChunk(ct, parents.get(pi), pi));
            }
        }

        return children;
    }

    private List<String> extractParagraphs(String content) {
        String[] raw = content.split("\\n\\s*\\n");
        List<String> result = new ArrayList<>();
        for (String p : raw) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<String> buildParentChunks(List<String> paragraphs, int parentSize) {
        List<String> parents = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() + 2 > parentSize && !current.isEmpty()) {
                parents.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(para);
        }

        if (!current.isEmpty()) {
            parents.add(current.toString().trim());
        }

        return parents;
    }

    private List<String> splitChildChunks(String text, int childSize) {
        List<String> children = new ArrayList<>();

        if (text.length() <= childSize) {
            children.add(text);
            return children;
        }

        // Split by lines within the parent chunk
        String[] lines = text.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (current.length() + trimmed.length() + 1 > childSize && !current.isEmpty()) {
                children.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty()) {
            children.add(current.toString().trim());
        }

        // Hard-split any child that still exceeds childSize
        List<String> result = new ArrayList<>();
        for (String child : children) {
            if (child.length() <= childSize) {
                result.add(child);
            } else {
                splitLongText(child, childSize, result);
            }
        }

        return result;
    }

    private void splitLongText(String text, int maxSize, List<String> result) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            end = findBreakBoundary(text, start, end);
            result.add(text.substring(start, end).trim());
            start = end;
        }
    }

    private int findBreakBoundary(String text, int start, int preferredEnd) {
        int end = Math.min(preferredEnd, text.length());
        // Walk backward to find a sentence boundary
        for (int i = end; i > start; i--) {
            char c = text.charAt(i - 1);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '.' || c == '!' || c == '?' || c == '\n') {
                return i;
            }
        }
        return end;
    }

    @Getter
    public static class ChildChunk {
        private final String content;
        private final String parentContent;
        private final int parentIndex;

        public ChildChunk(String content, String parentContent, int parentIndex) {
            this.content = content;
            this.parentContent = parentContent;
            this.parentIndex = parentIndex;
        }
    }
}
