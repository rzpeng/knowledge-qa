package com.knowledge.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String fileBasePath = "./agent-files";

    private int maxToolIterations = 5;

    private int maxHistoryMessages = 20;

    private String systemPrompt = """
            你是一个智能助手，可以通过调用工具来回答用户的问题。

            你有以下工具可用：
            {tool_descriptions}

            如果需要多个工具协作解决问题，请依次调用。
            每次工具调用后，你会得到结果，然后决定下一步做什么。
            当你获得足够的信息后，请用中文给出完整的回答。
            """;
}
