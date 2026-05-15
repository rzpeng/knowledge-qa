package com.knowledge.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.config.AgentProperties;
import com.knowledge.agent.entity.AgentMessage;
import com.knowledge.agent.tool.Tool;
import com.knowledge.agent.tool.ToolRegistry;
import com.knowledge.agent.tool.ToolResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final AgentSessionService sessionService;
    private final ToolRegistry toolRegistry;
    private final ChatLanguageModel chatModel;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;

    public String processMessage(Long sessionId, String userInput) {
        List<ChatMessage> messages = buildMessageList(sessionId, userInput);
        List<ToolSpecification> toolSpecs = toolRegistry.getSpecifications();
        String finalAnswer = executeAgentLoop(sessionId, messages, toolSpecs);

        AgentMessage userMsg = new AgentMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(userInput);
        sessionService.saveMessage(userMsg);

        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(finalAnswer);
        sessionService.saveMessage(assistantMsg);

        sessionService.updateSessionTime(sessionId);

        return finalAnswer;
    }

    private List<ChatMessage> buildMessageList(Long sessionId, String userInput) {
        List<ChatMessage> messages = new ArrayList<>();

        String toolDescriptions = buildToolDescriptions();
        String systemPrompt = agentProperties.getSystemPrompt()
                .replace("{tool_descriptions}", toolDescriptions);
        messages.add(SystemMessage.from(systemPrompt));

        List<AgentMessage> history = sessionService.getSessionMessages(sessionId);
        int maxHistory = agentProperties.getMaxHistoryMessages();
        if (history.size() > maxHistory) {
            history = history.subList(history.size() - maxHistory, history.size());
        }

        for (AgentMessage msg : history) {
            switch (msg.getRole()) {
                case "USER" -> messages.add(UserMessage.from(msg.getContent()));
                case "ASSISTANT" -> {
                    if (msg.getToolArgs() != null) {
                        List<ToolExecutionRequest> requests = parseToolRequests(msg.getToolArgs());
                        messages.add(AiMessage.from(requests));
                    } else {
                        messages.add(AiMessage.from(msg.getContent()));
                    }
                }
                case "TOOL" -> {
                    if (msg.getToolResult() != null) {
                        messages.add(new ToolExecutionResultMessage(
                                msg.getToolName(),
                                msg.getToolName(),
                                msg.getToolResult()
                        ));
                    }
                }
            }
        }

        messages.add(UserMessage.from(userInput));

        return messages;
    }

    private String executeAgentLoop(Long sessionId, List<ChatMessage> messages,
                                     List<ToolSpecification> toolSpecs) {
        int iterations = 0;
        int maxIterations = agentProperties.getMaxToolIterations();

        while (iterations < maxIterations) {
            Response<AiMessage> response = chatModel.generate(messages, toolSpecs);
            AiMessage aiMessage = response.content();

            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text() != null ? aiMessage.text() : "处理完成";
            }

            messages.add(aiMessage);
            saveToolCallMessage(sessionId, aiMessage.toolExecutionRequests());

            for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                try {
                    Tool tool = toolRegistry.getTool(request.name());
                    if (tool == null) {
                        String error = "工具 '" + request.name() + "' 不存在";
                        messages.add(new ToolExecutionResultMessage(
                                request.id(), request.name(), error));
                        saveToolResultMessage(sessionId, request.id(), request.name(), error);
                        continue;
                    }

                    Map<String, Object> args = objectMapper.readValue(
                            request.arguments(), new TypeReference<>() {});
                    ToolResult result = tool.execute(args);

                    String resultStr = result.isSuccess()
                            ? objectMapper.writeValueAsString(result.getData())
                            : "错误: " + result.getError();

                    messages.add(new ToolExecutionResultMessage(
                            request.id(), request.name(), resultStr));
                    saveToolResultMessage(sessionId, request.id(), request.name(), resultStr);
                } catch (Exception e) {
                    log.error("Tool execution error: {}", e.getMessage());
                    messages.add(new ToolExecutionResultMessage(
                            request.id(), request.name(), "执行异常: " + e.getMessage()));
                }
            }

            iterations++;
        }

        Response<AiMessage> finalResponse = chatModel.generate(messages, toolSpecs);
        return finalResponse.content().text() != null
                ? finalResponse.content().text()
                : "已达最大工具调用次数，请简化问题后重试。";
    }

    private void saveToolCallMessage(Long sessionId, List<ToolExecutionRequest> requests) {
        try {
            AgentMessage msg = new AgentMessage();
            msg.setSessionId(sessionId);
            msg.setRole("ASSISTANT");
            msg.setToolArgs(objectMapper.writeValueAsString(requests));
            sessionService.saveMessage(msg);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tool requests", e);
        }
    }

    private void saveToolResultMessage(Long sessionId, String toolCallId,
                                        String toolName, String result) {
        AgentMessage msg = new AgentMessage();
        msg.setSessionId(sessionId);
        msg.setRole("TOOL");
        msg.setToolName(toolName);
        msg.setToolResult(result);
        sessionService.saveMessage(msg);
    }

    private String buildToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (Tool tool : toolRegistry.getAllTools()) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
        }
        return sb.toString();
    }

    private List<ToolExecutionRequest> parseToolRequests(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse tool requests: {}", json, e);
            return List.of();
        }
    }
}
