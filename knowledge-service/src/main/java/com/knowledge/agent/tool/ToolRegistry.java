package com.knowledge.agent.tool;

import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            tools.put(tool.name(), tool);
        }
    }

    @PostConstruct
    public void init() {
        log.info("ToolRegistry initialized, {} tools registered", tools.size());
    }

    public void register(Tool tool) {
        if (tools.containsKey(tool.name())) {
            log.warn("Tool '{}' already registered, will be overwritten", tool.name());
        }
        tools.put(tool.name(), tool);
        log.debug("Tool '{}' registered", tool.name());
    }

    public void unregister(String name) {
        tools.remove(name);
        log.debug("Tool '{}' unregistered", name);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public List<Tool> getAllTools() {
        return List.copyOf(tools.values());
    }

    public List<ToolSpecification> getSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();
        for (Tool tool : tools.values()) {
            specs.add(toToolSpecification(tool));
        }
        return specs;
    }

    private ToolSpecification toToolSpecification(Tool tool) {
        Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (ToolParameter param : tool.parameters()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            String typeName = switch (param.getType()) {
                case STRING -> "string";
                case NUMBER -> "integer";
                case BOOLEAN -> "boolean";
            };
            prop.put("type", typeName);
            prop.put("description", param.getDescription());
            properties.put(param.getName(), prop);
            if (param.isRequired()) {
                required.add(param.getName());
            }
        }

        return ToolSpecification.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(ToolParameters.builder()
                        .properties(properties)
                        .build())
                .build();
    }
}
