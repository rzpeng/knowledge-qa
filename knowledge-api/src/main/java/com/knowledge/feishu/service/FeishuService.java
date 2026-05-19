package com.knowledge.feishu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.feishu.config.FeishuConfig;
import com.knowledge.feishu.entity.FeishuResponse;
import com.knowledge.agent.service.AgentOrchestratorService;
import com.knowledge.agent.service.AgentSessionService;
import com.knowledge.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeishuService {

    private static final String SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id";
    private static final String FEISHU_SESSION_PREFIX = "飞书-";

    @Autowired
    private FeishuConfig feishuConfig;

    @Autowired
    private FeishuTokenManager tokenManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatService chatService;

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @Autowired
    private AgentSessionService agentSessionService;

    private final ConcurrentHashMap<String, Long> sessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> agentSessionMap = new ConcurrentHashMap<>();

    public FeishuResponse sendTextMessage(String chatId, String text) {
        if (!feishuConfig.isEnabled()) {
            return FeishuResponse.error(1, "Feishu integration is disabled");
        }
        try {
            String contentJson = "{\"text\":\"" + escapeJson(text) + "\"}";

            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", chatId);
            body.put("msg_type", "text");
            body.put("content", contentJson);

            return callFeishuApi(body);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send text message: " + e.getMessage());
        }
    }

    public FeishuResponse sendCardMessage(String chatId, Map<String, Object> card) {
        if (!feishuConfig.isEnabled()) {
            return FeishuResponse.error(1, "Feishu integration is disabled");
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", chatId);
            body.put("msg_type", "interactive");
            body.put("content", objectMapper.writeValueAsString(card));

            return callFeishuApi(body);
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to send card message: " + e.getMessage());
        }
    }

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
     * 处理飞书群里的用户问题，调用 RAG 问答并回复
     */
    public void processQuestion(String chatId, String question) {
        if (!feishuConfig.isEnabled()) {
            log.warn("Feishu disabled, ignoring question from chat {}", chatId);
            return;
        }
        try {
            // 获取或创建会话
            Long sessionId = sessionMap.computeIfAbsent(chatId, id -> {
                var session = chatService.createSession(FEISHU_SESSION_PREFIX + id);
                log.info("Created chat session {} for feishu chat {}", session.getId(), id);
                return session.getId();
            });

            log.info("Feishu question from {}: {}", chatId, question);

            // 先发一条"正在思考"的提示
            sendTextMessage(chatId, "正在查询知识库，请稍候...");

            // 调用 RAG 问答
            String answer = chatService.chat(sessionId, question);

            log.info("Feishu answer to {}: {}", chatId, answer);

            // 发回答案
            sendKnowledgeAnswer(chatId, question, answer);

        } catch (Exception e) {
            log.error("Error processing feishu question from {}: {}", chatId, e.getMessage(), e);
            sendTextMessage(chatId, "抱歉，回答问题时出现了错误：" + e.getMessage());
        }
    }

    /**
     * 处理飞书群里的用户问题，调用智能助手并回复
     */
    public void processAgentQuestion(String chatId, String question) {
        if (!feishuConfig.isEnabled()) {
            log.warn("Feishu disabled, ignoring agent question from chat {}", chatId);
            return;
        }
        try {
            Long sessionId = agentSessionMap.computeIfAbsent(chatId, id -> {
                var session = agentSessionService.createSession("飞书-Agent-" + id);
                log.info("Created agent session {} for feishu chat {}", session.getId(), id);
                return session.getId();
            });

            log.info("Feishu agent question from {}: {}", chatId, question);

            sendTextMessage(chatId, "正在调用智能助手，请稍候...");

            String answer = agentOrchestratorService.processMessage(sessionId, question);

            log.info("Feishu agent answer to {}: {}", chatId, answer);

            sendKnowledgeAnswer(chatId, question, answer);

        } catch (Exception e) {
            log.error("Error processing feishu agent question from {}: {}", chatId, e.getMessage(), e);
            sendTextMessage(chatId, "抱歉，智能助手出现了错误：" + e.getMessage());
        }
    }

    private Map<String, Object> createKnowledgeAnswerCard(String question, String answer) {
        Map<String, Object> card = new HashMap<>();

        Map<String, Object> header = new HashMap<>();
        header.put("title", Map.of("tag", "plain_text", "content", "知识库问答结果"));
        header.put("template", "turquoise");

        Map<String, Object> questionField = new HashMap<>();
        questionField.put("is_short", false);
        questionField.put("text", Map.of("tag", "lark_md", "content", "**问题：** " + question));

        Map<String, Object> answerField = new HashMap<>();
        answerField.put("is_short", false);
        answerField.put("text", Map.of("tag", "lark_md", "content", "**答案：** " + answer));

        Map<String, Object> element = new HashMap<>();
        element.put("tag", "div");
        element.put("fields", new Object[]{questionField, answerField});

        Map<String, Object> action = new HashMap<>();
        action.put("tag", "button");
        action.put("text", Map.of("tag", "plain_text", "content", "查看详情"));
        action.put("type", "primary");
        action.put("url", "http://localhost:5173/chat");

        Map<String, Object> actions = new HashMap<>();
        actions.put("tag", "action");
        actions.put("actions", new Object[]{action});

        card.put("header", header);
        card.put("elements", new Object[]{element, actions});

        return card;
    }

    private FeishuResponse callFeishuApi(Map<String, Object> requestBody) {
        try {
            String token = tokenManager.getTenantAccessToken();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<FeishuApiResponse> response = restTemplate.exchange(
                    SEND_MESSAGE_URL,
                    HttpMethod.POST,
                    entity,
                    FeishuApiResponse.class
            );

            FeishuApiResponse apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getCode() == 0) {
                return FeishuResponse.success(apiResponse.getData());
            } else {
                String msg = apiResponse != null ? apiResponse.getMsg() : "no response";
                return FeishuResponse.error(apiResponse != null ? apiResponse.getCode() : -1,
                        "Feishu API error: " + msg);
            }
        } catch (Exception e) {
            return FeishuResponse.error(1, "Failed to call Feishu API: " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static class FeishuApiResponse {
        private int code;
        private String msg;
        private Object data;

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
