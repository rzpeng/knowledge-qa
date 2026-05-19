package com.knowledge.feishu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.feishu.entity.FeishuResponse;
import com.knowledge.feishu.service.FeishuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
public class FeishuWebhookController {

    @Autowired
    private FeishuService feishuService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 飞书Webhook消息接收端点
     * 处理 URL 验证挑战和真实事件推送
     */
    @PostMapping("/webhook")
    public Object handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // 处理飞书 URL 验证挑战
            if ("url_verify".equals(payload.get("type"))) {
                return Collections.singletonMap("challenge", payload.get("challenge"));
            }

            // 解析真实事件格式
            Map<String, Object> header = (Map<String, Object>) payload.get("header");
            Map<String, Object> event = (Map<String, Object>) payload.get("event");

            if (header == null || event == null) {
                return FeishuResponse.error(1, "Invalid webhook payload");
            }

            String eventType = (String) header.get("event_type");
            Map<String, Object> message = (Map<String, Object>) event.get("message");

            if (message == null) {
                return FeishuResponse.error(1, "No message in event");
            }

            String chatId = (String) message.get("chat_id");
            String msgType = (String) message.get("message_type");
            String contentStr = (String) message.get("content");

            // 只处理文本消息
            if (!"text".equals(msgType) || contentStr == null) {
                return FeishuResponse.error(1, "Unsupported message type: " + msgType);
            }

            // 解析 content JSON 提取用户问题
            @SuppressWarnings("unchecked")
            Map<String, String> content = objectMapper.readValue(contentStr, Map.class);
            String question = content != null ? content.get("text") : null;

            if (question == null || question.isBlank()) {
                return FeishuResponse.error(1, "Empty question");
            }

            // 调用 RAG 问答处理
            feishuService.processQuestion(chatId, question);

            // 飞书事件回调期望返回空 200
            return Collections.singletonMap("code", 0);

        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to process webhook: " + e.getMessage());
        }
    }

    @PostMapping("/message/text")
    public FeishuResponse sendTextMessage(@RequestParam String chatId, @RequestParam String text) {
        return feishuService.sendTextMessage(chatId, text);
    }

    @PostMapping("/message/card")
    public FeishuResponse sendCardMessage(@RequestParam String chatId, @RequestBody Map<String, Object> card) {
        return feishuService.sendCardMessage(chatId, card);
    }

    @PostMapping("/message/knowledge")
    public FeishuResponse sendKnowledgeAnswer(@RequestParam String chatId,
                                           @RequestParam String question,
                                           @RequestParam String answer) {
        return feishuService.sendKnowledgeAnswer(chatId, question, answer);
    }

    @GetMapping("/health")
    public FeishuResponse health() {
        return FeishuResponse.success("Feishu integration is running");
    }
}