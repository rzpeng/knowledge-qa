package com.knowledge.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FunctionToolSpecConverter {

    private final ObjectMapper objectMapper;

    public Map<String, Object> parseArgs(String arguments) {
        try {
            return objectMapper.readValue(arguments, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse tool arguments: " + arguments, e);
        }
    }

    public String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tool result", e);
        }
    }
}
