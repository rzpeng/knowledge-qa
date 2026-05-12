package com.knowledge.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuApiTestService {

    private final EmbeddingModel embeddingModel;

    public void testBatchSize() {
        int[] batchSizes = {1, 3, 5, 10, 20, 50};

        for (int size : batchSizes) {
            log.info("========== 测试批量大小: {} ==========", size);
            try {
                List<TextSegment> segments = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    segments.add(TextSegment.from("测试文本" + (i + 1)));
                }

                long startTime = System.currentTimeMillis();
                var response = embeddingModel.embedAll(segments);
                long endTime = System.currentTimeMillis();

                log.info("✅ 成功！批量大小: {}, 耗时: {}ms, 向量数: {}",
                        size, endTime - startTime, response.content().size());

                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("❌ 失败！批量大小: {}, 错误: {}", size, e.getMessage());
            }
        }
    }
}
