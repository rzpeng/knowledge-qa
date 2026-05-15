package com.knowledge.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private boolean success;
    private Object data;
    private String error;

    public static ToolResult ok(Object data) {
        return ToolResult.builder().success(true).data(data).build();
    }

    public static ToolResult fail(String error) {
        return ToolResult.builder().success(false).error(error).build();
    }
}
