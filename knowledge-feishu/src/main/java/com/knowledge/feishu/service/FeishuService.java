package com.knowledge.feishu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.feishu.config.FeishuConfig;
import com.knowledge.feishu.entity.FeishuMessage;
import com.knowledge.feishu.entity.FeishuResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FeishuService {
    
    @Autowired
    private FeishuConfig feishuConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 发送文本消息
     */
    public FeishuResponse sendTextMessage(String chatId, String text) {
        if (!feishuConfig.isEnabled()) {
            return FeishuResponse.error(1, "Feishu integration is disabled");
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");
            message.put("content", Map.of("text", text));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("msg_type", "text");
            requestBody.put("content", message.get("content"));
            
            return sendMessage(requestBody);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send text message: " + e.getMessage());
        }
    }
    
    /**
     * 发送卡片消息
     */
    public FeishuResponse sendCardMessage(String chatId, Map<String, Object> card) {
        if (!feishuConfig.isEnabled()) {
            return FeishuResponse.error(1, "Feishu integration is disabled");
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "interactive");
            message.put("card", card);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("msg_type", "interactive");
            requestBody.put("card", card);
            
            return sendMessage(requestBody);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send card message: " + e.getMessage());
        }
    }
    
    /**
     * 发送知识库问答结果
     */
    public FeishuResponse sendKnowledgeAnswer(String chatId, String question, String answer) {
        if (!feishuConfig.isEnabled()) {
            return FeishuResponse.error(1, "Feishu integration is disabled");
        }
        
        try {
            Map<String, Object> card = createKnowledgeAnswerCard(question, answer);
            return sendCardMessage(chatId, card);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send knowledge answer: " + e.getMessage());
        }
    }
    
    /**
     * 创建知识库问答卡片
     */
    private Map<String, Object> createKnowledgeAnswerCard(String question, String answer) {
        Map<String, Object> card = new HashMap<>();
        
        Map<String, Object> header = new HashMap<>();
        header.put("title", Map.of("tag", "plain_text", "content", "知识库问答结果"));
        header.put("template", "turquoise");
        
        Map<String, Object> element = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        Map<String, Object> questionField = new HashMap<>();
        questionField.put("tag", "plain_text");
        questionField.put("content", "问题: " + question);
        fields.put("question", questionField);
        
        Map<String, Object> answerField = new HashMap<>();
        answerField.put("tag", "plain_text");
        answerField.put("content", "答案: " + answer);
        fields.put("answer", answerField);
        
        element.put("tag", "div");
        element.put("fields", fields);
        
        Map<String, Object> actions = new HashMap<>();
        Map<String, Object> action = new HashMap<>();
        action.put("tag", "button");
        action.put("text", Map.of("tag", "plain_text", "content", "查看详情"));
        action.put("type", "primary");
        action.put("url", "http://localhost:5173/chat");
        actions.put("action", action);
        
        card.put("header", header);
        card.put("elements", new Object[]{element, actions});
        
        return card;
    }
    
    /**
     * 发送消息到飞书API
     */
    private FeishuResponse sendMessage(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + feishuConfig.getBot_token());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<FeishuResponse> response = restTemplate.exchange(
                feishuConfig.getWebhook_url(),
                HttpMethod.POST,
                entity,
                FeishuResponse.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send message to Feishu: " + e.getMessage());
        }
    }
    
    /**
     * 处理飞书Webhook消息
     */
    public Map<String, Object> processWebhookMessage(FeishuMessage message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 根据消息类型处理
            switch (message.getMsg_type()) {
                case "text":
                    result = handleTextMessage(message);
                    break;
                case "interactive":
                    result = handleInteractiveMessage(message);
                    break;
                default:
                    result.put("code", 1);
                    result.put("msg", "Unsupported message type: " + message.getMsg_type());
            }
        } catch (Exception e) {
            result.put("code", 1);
            result.put("msg", "Error processing message: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 处理文本消息
     */
    private Map<String, Object> handleTextMessage(FeishuMessage message) {
        Map<String, Object> result = new HashMap<>();
        
        // 这里可以集成到现有的聊天服务
        String text = message.getText();
        String chatId = message.getChat_id();
        
        // 返回确认消息
        result.put("msg_type", "text");
        result.put("content", Map.of("text", "收到您的消息: " + text));
        
        return result;
    }
    
    /**
     * 处理交互式消息
     */
    private Map<String, Object> handleInteractiveMessage(FeishuMessage message) {
        Map<String, Object> result = new HashMap<>();
        
        // 处理卡片交互
        result.put("code", 0);
        result.put("msg", "success");
        
        return result;
    }
}