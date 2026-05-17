package com.knowledge.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class DeepSeekAiConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-v4-flash}")
    private String model;

    @Value("${deepseek.agent-model:deepseek-chat}")
    private String agentModel;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Bean
    @Primary
    @Qualifier("deepseekChatModel")
    public OpenAiChatModel deepseekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @Qualifier("agentChatModel")
    public OpenAiChatModel agentChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(agentModel)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
