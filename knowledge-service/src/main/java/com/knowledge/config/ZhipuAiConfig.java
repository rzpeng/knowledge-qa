package com.knowledge.config;

import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.time.Duration;

@Configuration
public class ZhipuAiConfig {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("${zhipu.model:glm-4.5-air}")
    private String model;

    @Value("${zhipu.embedding-model:embedding-3}")
    private String embeddingModel;

    @Value("${zhipu.embedding-dimensions:1024}")
    private int embeddingDimensions;

    @Bean
    public ZhipuAiChatModel chatModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(model)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @Primary
    public ZhipuAiEmbeddingModel embeddingModel() {
        return ZhipuAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .model(embeddingModel)
                .dimensions(embeddingDimensions)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }
}
