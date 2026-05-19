package com.knowledge.feishu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.feishu.config.FeishuConfig;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.lark.oapi.ws.Client;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class FeishuWebSocketClient {

    @Autowired
    private FeishuConfig feishuConfig;

    @Autowired
    private FeishuService feishuService;

    @Autowired
    private ObjectMapper objectMapper;

    private Client wsClient;

    @PostConstruct
    public void start() {
        if (!feishuConfig.isEnabled()) {
            log.info("Feishu disabled, WebSocket not started");
            return;
        }

        try {
            EventDispatcher eventHandler = EventDispatcher.newBuilder("", "")
                    .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                        @Override
                        public void handle(P2MessageReceiveV1 event) throws Exception {
                            P2MessageReceiveV1Data data = event.getEvent();
                            if (data == null) return;

                            EventMessage message = data.getMessage();
                            if (message == null) return;

                            String chatId = message.getChatId();
                            String msgType = message.getMessageType();
                            String content = message.getContent();

                            if (!"text".equals(msgType) || content == null) {
                                return;
                            }

                            @SuppressWarnings("unchecked")
                            Map<String, String> contentMap = objectMapper.readValue(content, Map.class);
                            String question = contentMap != null ? contentMap.get("text") : null;

                            if (question == null || question.isBlank()) {
                                return;
                            }

                            // 以 /agent 或 "智能助手" 开头的问题走智能助手，否则走知识库 RAG
                            if (question.startsWith("/agent") || question.startsWith("智能助手")) {
                                feishuService.processAgentQuestion(chatId, question);
                            } else {
                                feishuService.processQuestion(chatId, question);
                            }
                        }
                    })
                    .build();

            wsClient = new Client.Builder(feishuConfig.getApp_id(), feishuConfig.getApp_secret())
                    .eventHandler(eventHandler)
                    .build();

            wsClient.start();
            log.info("Feishu WebSocket client started successfully");
        } catch (Exception e) {
            log.error("Failed to start Feishu WebSocket client", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (wsClient != null) {
            try {
                log.info("Feishu WebSocket client shutting down");
            } catch (Exception e) {
                log.warn("Error stopping Feishu WS client", e);
            }
        }
    }
}
