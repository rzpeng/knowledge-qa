package com.knowledge.agent.tool;

import java.util.List;
import java.util.Map;

public interface Tool {
    String name();
    String description();
    List<ToolParameter> parameters();
    ToolResult execute(Map<String, Object> args);
}
