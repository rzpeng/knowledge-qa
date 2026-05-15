package com.knowledge.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameter {
    private String name;
    private String description;
    private ParamType type;
    private boolean required;
}
