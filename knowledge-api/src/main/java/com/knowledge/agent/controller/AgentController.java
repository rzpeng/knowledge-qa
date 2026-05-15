package com.knowledge.agent.controller;

import com.knowledge.agent.dto.AgentChatRequest;
import com.knowledge.agent.dto.AgentMessageDTO;
import com.knowledge.agent.dto.AgentSessionDTO;
import com.knowledge.agent.dto.ToolInfoDTO;
import com.knowledge.agent.entity.AgentMessage;
import com.knowledge.agent.entity.AgentSession;
import com.knowledge.agent.service.AgentOrchestratorService;
import com.knowledge.agent.service.AgentSessionService;
import com.knowledge.agent.tool.Tool;
import com.knowledge.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentSessionService sessionService;
    private final AgentOrchestratorService orchestratorService;
    private final ToolRegistry toolRegistry;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/sessions")
    public ResponseEntity<AgentSessionDTO> createSession(@RequestBody Map<String, String> request) {
        AgentSession session = sessionService.createSession(request.get("title"));
        return ResponseEntity.ok(toSessionDTO(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AgentSessionDTO>> listSessions() {
        List<AgentSession> sessions = sessionService.listSessions();
        List<AgentSessionDTO> dtos = sessions.stream()
                .map(this::toSessionDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody AgentChatRequest request) {
        String answer = orchestratorService.processMessage(
                request.getSessionId(), request.getQuestion());
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<AgentMessageDTO>> getMessages(@PathVariable Long id) {
        List<AgentMessage> messages = sessionService.getSessionMessages(id);
        List<AgentMessageDTO> dtos = messages.stream()
                .map(this::toMessageDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/tools")
    public ResponseEntity<List<ToolInfoDTO>> listTools() {
        List<ToolInfoDTO> dtos = toolRegistry.getAllTools().stream()
                .map(this::toToolInfoDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam Long sessionId, @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(60000L);
        executor.execute(() -> {
            try {
                String answer = orchestratorService.processMessage(sessionId, question);
                emitter.send(SseEmitter.event().name("message").data(answer));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private AgentSessionDTO toSessionDTO(AgentSession session) {
        AgentSessionDTO dto = new AgentSessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setCreateTime(session.getCreateTime());
        dto.setUpdateTime(session.getUpdateTime());
        return dto;
    }

    private AgentMessageDTO toMessageDTO(AgentMessage msg) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setId(msg.getId());
        dto.setSessionId(msg.getSessionId());
        dto.setRole(msg.getRole());
        dto.setContent(msg.getContent());
        dto.setToolName(msg.getToolName());
        dto.setToolArgs(msg.getToolArgs());
        dto.setToolResult(msg.getToolResult());
        dto.setCreateTime(msg.getCreateTime());
        return dto;
    }

    private ToolInfoDTO toToolInfoDTO(Tool tool) {
        ToolInfoDTO dto = new ToolInfoDTO();
        dto.setName(tool.name());
        dto.setDescription(tool.description());
        dto.setParameters(tool.parameters().stream()
                .map(p -> {
                    ToolInfoDTO.ParamInfo pi = new ToolInfoDTO.ParamInfo();
                    pi.setName(p.getName());
                    pi.setDescription(p.getDescription());
                    pi.setType(p.getType().name());
                    pi.setRequired(p.isRequired());
                    return pi;
                })
                .toList());
        return dto;
    }
}
