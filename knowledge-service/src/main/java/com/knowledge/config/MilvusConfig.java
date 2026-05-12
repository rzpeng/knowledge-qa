package com.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        try {
            return new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(host)
                            .withPort(port)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to connect to Milvus at {}:{}: {}", host, port, e.getMessage());
            log.warn("Milvus is not available. Document processing and AI search features will be disabled.");
            return null;
        }
    }
}
