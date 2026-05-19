package com.knowledge.feishu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.feishu.entity.FeishuMessage;
import com.knowledge.feishu.entity.FeishuResponse;
import com.knowledge.feishu.service.FeishuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
@CrossOrigin(origins = "*")
public class FeishuWebhookController {
    
    @Autowired
    private FeishuService feishuService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 飞书Webhook消息接收端点
     */
    @PostMapping("/webhook")
    public FeishuResponse handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // 解析飞书消息
            FeishuMessage message = objectMapper.convertValue(payload, FeishuMessage.class);
            
            // 处理消息
            Map<String, Object> result = feishuService.processWebhookMessage(message);
            
            return FeishuResponse.success(result);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to process webhook message: " + e.getMessage());
        }
    }
    
    /**
     * 发送文本消息
     */
    @PostMapping("/message/text")
    public FeishuResponse sendTextMessage(@RequestParam String chatId, @RequestParam String text) {
        return feishuService.sendTextMessage(chatId, text);
    }
    
    /**
     * 发送卡片消息
     */
    @PostMapping("/message/card")
    public FeishuResponse sendCardMessage(@RequestParam String chatId, @RequestBody Map<String, Object> card) {
        return feishuService.sendCardMessage(chatId, card);
    }
    
    /**
     * 发送知识库问答结果
     */
    @PostMapping("/message/knowledge")
    public FeishuResponse sendKnowledgeAnswer(@RequestParam String chatId, 
                                           @RequestParam String question, 
                                           @RequestParam String answer) {
        return feishuService.sendKnowledgeAnswer(chatId, question, answer);
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public FeishuResponse health() {
        return FeishuResponse.success("Feishu integration is running");
    }
}