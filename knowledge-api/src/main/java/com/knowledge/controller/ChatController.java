package com.knowledge.controller;

import cn.hutool.core.bean.BeanUtil;
import com.knowledge.dto.ChatMessageDTO;
import com.knowledge.dto.ChatRequest;
import com.knowledge.dto.ChatSessionDTO;
import com.knowledge.entity.ChatMessage;
import com.knowledge.entity.ChatSession;
import com.knowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(@RequestBody Map<String, String> request) {
        String title = request.getOrDefault("title", "新对话");
        ChatSession session = chatService.createSession(title);
        return ResponseEntity.ok(toSessionDTO(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDTO>> listSessions() {
        List<ChatSession> sessions = chatService.listSessions();
        List<ChatSessionDTO> dtos = sessions.stream()
                .map(this::toSessionDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Long id) {
        List<ChatMessage> messages = chatService.getSessionMessages(id);
        List<ChatMessageDTO> dtos = messages.stream()
                .map(this::toMessageDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.getSessionId(), request.getQuestion());
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam Long sessionId,
            @RequestParam String question) {

        SseEmitter emitter = new SseEmitter(60000L);

        executor.execute(() -> {
            try {
                String answer = chatService.chat(sessionId, question);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(answer));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private ChatSessionDTO toSessionDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        BeanUtil.copyProperties(session, dto);
        return dto;
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        BeanUtil.copyProperties(message, dto);
        return dto;
    }
}
