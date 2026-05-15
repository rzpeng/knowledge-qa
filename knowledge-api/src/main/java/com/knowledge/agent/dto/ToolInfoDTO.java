package com.knowledge.agent.dto;

import lombok.Data;
import java.util.List;

@Data
public class ToolInfoDTO {
    private String name;
    private String description;
    private List<ParamInfo> parameters;

    @Data
    public static class ParamInfo {
        private String name;
        private String description;
        private String type;
        private boolean required;
    }
}
