# Tool-Calling Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tool-calling intelligent assistant module to the existing knowledge-qa RAG system, supporting weather query, calculator, database query, file operation, and multi-tool chaining via natural language.

**Architecture:** Custom `Tool` interface + `ToolRegistry` for tool management, LangChain4j `ToolSpecification` for OpenAI-compatible function calling protocol with DeepSeek model, independent REST endpoints (`/api/agent/*`) with separate session/message tables.

**Tech Stack:** Spring Boot 3.2.5, LangChain4j 0.34.0, DeepSeek (OpenAI-compatible API), MyBatis-Plus 3.5.9, Hutool 5.8.26

---

### Task 1: Tool interface definitions (knowledge-core)

**Files:** Create 4 new files in `knowledge-core/src/main/java/com/knowledge/agent/tool/`:

**Tool.java**
```java
package com.knowledge.agent.tool;

import java.util.List;
import java.util.Map;

public interface Tool {
    String name();
    String description();
    List<ToolParameter> parameters();
    ToolResult execute(Map<String, Object> args);
}
```

**ToolParameter.java**
```java
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
```

**ParamType.java**
```java
package com.knowledge.agent.tool;

public enum ParamType {
    STRING, NUMBER, BOOLEAN
}
```

**ToolResult.java**
```java
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
```

- [ ] **Step 1: Create Tool.java interface**
- [ ] **Step 2: Create ToolParameter.java**
- [ ] **Step 3: Create ParamType.java enum**
- [ ] **Step 4: Create ToolResult.java**
- [ ] **Step 5: Commit**

```
git add knowledge-core/src/main/java/com/knowledge/agent/tool/
git commit -m "feat: add tool interface definitions"
```

---

### Task 2: Agent entities and mappers (knowledge-core)

**Files:** Create 4 new files:

**`knowledge-core/src/main/java/com/knowledge/agent/entity/AgentSession.java`**
```java
package com.knowledge.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**`knowledge-core/src/main/java/com/knowledge/agent/entity/AgentMessage.java`**
```java
package com.knowledge.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String role;          // USER / ASSISTANT / TOOL
    private String content;       // text content
    private String toolName;      // tool name for TOOL role
    private String toolArgs;      // JSON arguments for ASSISTANT with tool calls
    private String toolResult;    // JSON result for TOOL role
    private LocalDateTime createTime;
}
```
Note: For an ASSISTANT message with tool calls, `content` is null and `toolArgs` stores the JSON array string of all tool calls from that AI response turn. For a TOOL message, `toolName` and `toolResult` hold the execution info, and `content` is null.

**`knowledge-core/src/main/java/com/knowledge/agent/mapper/AgentSessionMapper.java`**
```java
package com.knowledge.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {}
```

**`knowledge-core/src/main/java/com/knowledge/agent/mapper/AgentMessageMapper.java`**
```java
package com.knowledge.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {}
```

- [ ] **Step 1: Create AgentSession entity**
- [ ] **Step 2: Create AgentMessage entity**
- [ ] **Step 3: Create AgentSessionMapper**
- [ ] **Step 4: Create AgentMessageMapper**
- [ ] **Step 5: Commit**

```
git add knowledge-core/src/main/java/com/knowledge/agent/entity/
git add knowledge-core/src/main/java/com/knowledge/agent/mapper/
git commit -m "feat: add agent session and message entities with mappers"
```

---

### Task 3: Tool registry and converter (knowledge-service)

**Files:** Create 2 files:

**`knowledge-service/src/main/java/com/knowledge/agent/tool/ToolRegistry.java`**
```java
package com.knowledge.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

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
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        Map<String, JsonObjectElement> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (ToolParameter param : tool.parameters()) {
            JsonValueElement element = switch (param.getType()) {
                case STRING -> JsonStringSchema.builder().description(param.getDescription()).build();
                case NUMBER -> JsonIntegerSchema.builder().description(param.getDescription()).build();
                case BOOLEAN -> JsonBooleanSchema.builder().description(param.getDescription()).build();
            };
            properties.put(param.getName(), element);
            if (param.isRequired()) {
                required.add(param.getName());
            }
        }

        schemaBuilder.addProperties(properties);
        if (!required.isEmpty()) {
            schemaBuilder.required(required);
        }

        return ToolSpecification.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(schemaBuilder.build())
                .build();
    }
}
```

**`knowledge-service/src/main/java/com/knowledge/agent/tool/FunctionToolSpecConverter.java`**
```java
package com.knowledge.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
```

- [ ] **Step 1: Create ToolRegistry.java** (with ToolSpecification conversion logic)
- [ ] **Step 2: Create FunctionToolSpecConverter.java**
- [ ] **Step 3: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/tool/
git commit -m "feat: add tool registry and function spec converter"
```

---

### Task 4: Agent configuration properties (knowledge-service)

**File:** `knowledge-service/src/main/java/com/knowledge/agent/config/AgentProperties.java`

```java
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
```

Also add to `application.yml`:
```yaml
agent:
  file-base-path: ./agent-files
  max-tool-iterations: 5
  max-history-messages: 20
```

- [ ] **Step 1: Create AgentProperties.java**
- [ ] **Step 2: Add agent config to application.yml**
- [ ] **Step 3: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/config/
git commit -m "feat: add agent configuration properties"
```

---

### Task 5: Weather tool implementation (knowledge-service)

**File:** `knowledge-service/src/main/java/com/knowledge/agent/tool/WeatherTool.java`

```java
package com.knowledge.agent.tool;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool implements Tool {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() { return "weather_query"; }

    @Override
    public String description() {
        return "查询指定城市的天气情况，返回温度、天气状况、湿度、风速等信息。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("city")
                        .description("城市名称，如：北京、上海、广州")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("date")
                        .description("日期，格式为YYYY-MM-DD，默认为今天")
                        .type(ParamType.STRING)
                        .required(false)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String city = (String) args.get("city");
            // Use Open-Meteo geocoding to get coordinates
            double[] coords = getCoordinates(city);
            if (coords == null) {
                return ToolResult.fail("未找到城市: " + city);
            }

            String url = WEATHER_API + "?latitude=" + coords[0]
                    + "&longitude=" + coords[1]
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                    + "&timezone=Asia/Shanghai";

            String response = HttpUtil.get(url, 5000);
            JsonNode root = objectMapper.readTree(response);
            JsonNode current = root.get("current");

            String result = String.format(
                    "城市: %s\n当前温度: %s°C\n湿度: %s%%\n风速: %s km/h\n天气代码: %s",
                    city,
                    current.get("temperature_2m").asText(),
                    current.get("relative_humidity_2m").asText(),
                    current.get("wind_speed_10m").asText(),
                    current.get("weather_code").asText()
            );

            return ToolResult.ok(result);
        } catch (Exception e) {
            log.error("Weather query failed", e);
            return ToolResult.fail("天气查询失败: " + e.getMessage());
        }
    }

    private double[] getCoordinates(String city) {
        // Simple city → coordinate mapping
        Map<String, double[]> cityMap = Map.of(
                "北京", new double[]{39.9042, 116.4074},
                "上海", new double[]{31.2304, 121.4737},
                "广州", new double[]{23.1291, 113.2644},
                "深圳", new double[]{22.5431, 114.0579},
                "杭州", new double[]{30.2741, 120.1551},
                "成都", new double[]{30.5728, 104.0668},
                "武汉", new double[]{30.5928, 114.3055},
                "南京", new double[]{32.0603, 118.7969},
                "西安", new double[]{34.3416, 108.9398},
                "重庆", new double[]{29.4316, 106.9123}
        );
        double[] coords = cityMap.get(city);
        if (coords != null) return coords;
        // Try to match substring (e.g., "北京市" → "北京")
        for (Map.Entry<String, double[]> entry : cityMap.entrySet()) {
            if (city.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
```

- [ ] **Step 1: Create WeatherTool.java**
- [ ] **Step 2: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/tool/WeatherTool.java
git commit -m "feat: add weather query tool"
```

---

### Task 6: Calculator tool implementation (knowledge-service)

**File:** `knowledge-service/src/main/java/com/knowledge/agent/tool/CalculatorTool.java`

```java
package com.knowledge.agent.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculatorTool implements Tool {

    private final ToolRegistry toolRegistry;
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() { return "calculator"; }

    @Override
    public String description() {
        return "执行数学计算，支持加(+)、减(-)、乘(*)、除(/)、幂(^或**)、括号等运算。传入数学表达式，返回计算结果。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("expression")
                        .description("数学表达式，如：25 + 10 * 2")
                        .type(ParamType.STRING)
                        .required(true)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String expression = (String) args.get("expression");
            // Sanitize: only allow safe characters
            if (!expression.matches("[0-9+\\-*/^().,%\\s]+")) {
                return ToolResult.fail("表达式包含非法字符");
            }
            // Replace ^ with ** for JavaScript pow operator
            String safeExpr = expression.replace("^", "**");
            Object result = engine.eval(safeExpr);
            return ToolResult.ok(expression + " = " + result);
        } catch (ScriptException e) {
            return ToolResult.fail("计算错误: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 1: Create CalculatorTool.java**
- [ ] **Step 2: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/tool/CalculatorTool.java
git commit -m "feat: add calculator tool"
```

---

### Task 7: Database query tool implementation (knowledge-service)

**File:** `knowledge-service/src/main/java/com/knowledge/agent/tool/DatabaseQueryTool.java`

```java
package com.knowledge.agent.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseQueryTool implements Tool {

    private final ToolRegistry toolRegistry;
    private final JdbcTemplate jdbcTemplate;

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*SELECT\\s+.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int MAX_ROWS = 100;

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() { return "database_query"; }

    @Override
    public String description() {
        return "执行数据库查询，仅支持SELECT语句。返回查询结果表格。可用于查询业务数据。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("sql")
                        .description("SQL查询语句，必须为SELECT开头")
                        .type(ParamType.STRING)
                        .required(true)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String sql = ((String) args.get("sql")).trim();
            if (!SELECT_PATTERN.matcher(sql).matches()) {
                return ToolResult.fail("仅允许执行SELECT查询");
            }
            // Block dangerous operations embedded in SELECT
            String upper = sql.toUpperCase();
            if (upper.contains("INTO OUTFILE") || upper.contains("INTO DUMPFILE")
                    || upper.contains("LOAD_FILE") || upper.contains("SLEEP(")) {
                return ToolResult.fail("查询包含不允许的操作");
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.size() > MAX_ROWS) {
                rows = rows.subList(0, MAX_ROWS);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> safeRow = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    safeRow.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "NULL");
                }
                result.add(safeRow);
            }

            return ToolResult.ok(Map.of(
                    "columns", rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet()),
                    "rows", result,
                    "total", result.size()
            ));
        } catch (Exception e) {
            return ToolResult.fail("查询失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 1: Create DatabaseQueryTool.java**
- [ ] **Step 2: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/tool/DatabaseQueryTool.java
git commit -m "feat: add database query tool"
```

---

### Task 8: File operation tool implementation (knowledge-service)

**File:** `knowledge-service/src/main/java/com/knowledge/agent/tool/FileOperationTool.java`

```java
package com.knowledge.agent.tool;

import com.knowledge.agent.config.AgentProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileOperationTool implements Tool {

    private final ToolRegistry toolRegistry;
    private final AgentProperties agentProperties;

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() { return "file_operation"; }

    @Override
    public String description() {
        return "文件读写操作，支持读取文件内容(read)、写入文件(write)、列出目录文件(list)。所有操作在安全沙箱目录内执行。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("operation")
                        .description("操作类型：read（读取文件）、write（写入文件）、list（列出目录）")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("path")
                        .description("文件路径（相对于沙箱根目录），如：test.txt、data/report.json")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("content")
                        .description("写入内容（仅在write操作时使用）")
                        .type(ParamType.STRING)
                        .required(false)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String operation = (String) args.get("operation");
            String path = (String) args.get("path");

            Path baseDir = Paths.get(agentProperties.getFileBasePath()).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            Path targetPath = baseDir.resolve(path).normalize();
            // Anti path traversal: ensure the resolved path is still within base dir
            if (!targetPath.startsWith(baseDir)) {
                return ToolResult.fail("路径不允许越界访问");
            }

            return switch (operation) {
                case "read" -> readFile(targetPath);
                case "write" -> {
                    String content = (String) args.get("content");
                    if (content == null) {
                        yield ToolResult.fail("写入操作需要提供content参数");
                    }
                    yield writeFile(targetPath, content);
                }
                case "list" -> listFiles(targetPath);
                default -> ToolResult.fail("不支持的操作: " + operation);
            };
        } catch (Exception e) {
            return ToolResult.fail("文件操作失败: " + e.getMessage());
        }
    }

    private ToolResult readFile(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ToolResult.fail("文件不存在: " + path.getFileName());
        }
        String content = Files.readString(path);
        return ToolResult.ok(Map.of("path", path.toString(), "content", content, "size", content.length()));
    }

    private ToolResult writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return ToolResult.ok(Map.of("path", path.toString(), "size", content.length(), "status", "written"));
    }

    private ToolResult listFiles(Path path) throws IOException {
        if (!Files.exists(path)) {
            return ToolResult.fail("路径不存在: " + path.getFileName());
        }
        List<Map<String, Object>> files;
        try (Stream<Path> stream = Files.list(path)) {
            files = stream.map(p -> {
                try {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", p.getFileName().toString());
                    info.put("type", Files.isDirectory(p) ? "directory" : "file");
                    info.put("size", Files.size(p));
                    return info;
                } catch (IOException e) {
                    return Map.of("name", p.getFileName().toString(), "type", "unknown");
                }
            }).collect(Collectors.toList());
        }
        return ToolResult.ok(Map.of("path", path.toString(), "files", files, "total", files.size()));
    }
}
```

- [ ] **Step 1: Create FileOperationTool.java**
- [ ] **Step 2: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/tool/FileOperationTool.java
git commit -m "feat: add file operation tool"
```

---

### Task 9: Agent session service and orchestration (knowledge-service)

**Files:** Create 2 files:

**`knowledge-service/src/main/java/com/knowledge/agent/service/AgentSessionService.java`**

Agent session CRUD operations, mirroring ChatService pattern but for agent sessions:

```java
package com.knowledge.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.agent.entity.AgentMessage;
import com.knowledge.agent.entity.AgentSession;
import com.knowledge.agent.mapper.AgentMessageMapper;
import com.knowledge.agent.mapper.AgentSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentSessionService {

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;

    public AgentSession createSession(String title) {
        AgentSession session = new AgentSession();
        session.setTitle(title != null ? title : "Agent对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public List<AgentSession> listSessions() {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<AgentSession>()
                        .orderByDesc(AgentSession::getUpdateTime));
    }

    public List<AgentMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
                        .orderByAsc(AgentMessage::getCreateTime));
    }

    public void deleteSession(Long sessionId) {
        messageMapper.delete(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    public void saveMessage(AgentMessage msg) {
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    public void updateSessionTime(Long sessionId) {
        AgentSession session = new AgentSession();
        session.setId(sessionId);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }
}
```

**`knowledge-service/src/main/java/com/knowledge/agent/service/AgentOrchestratorService.java`**

Core agent loop with tool calling:

```java
package com.knowledge.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.config.AgentProperties;
import com.knowledge.agent.entity.AgentMessage;
import com.knowledge.agent.tool.*;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

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
        // 1. Build message list
        List<ChatMessage> messages = buildMessageList(sessionId, userInput);

        // 2. Get tool specifications
        List<ToolSpecification> toolSpecs = toolRegistry.getSpecifications();

        // 3. Execute agent loop
        String finalAnswer = executeAgentLoop(sessionId, messages, toolSpecs);

        // 4. Save user message
        AgentMessage userMsg = new AgentMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(userInput);
        sessionService.saveMessage(userMsg);

        // 5. Save assistant response
        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(finalAnswer);
        sessionService.saveMessage(assistantMsg);

        // 6. Update session timestamp
        sessionService.updateSessionTime(sessionId);

        return finalAnswer;
    }

    private List<ChatMessage> buildMessageList(Long sessionId, String userInput) {
        List<ChatMessage> messages = new ArrayList<>();

        // System prompt with tool descriptions
        String toolDescriptions = buildToolDescriptions();
        String systemPrompt = agentProperties.getSystemPrompt()
                .replace("{tool_descriptions}", toolDescriptions);
        messages.add(SystemMessage.from(systemPrompt));

        // History (recent N messages)
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
                        // Reconstruct AiMessage with tool execution requests
                        List<ToolExecutionRequest> requests = parseToolRequests(msg.getToolArgs());
                        messages.add(AiMessage.from(requests));
                    } else {
                        messages.add(AiMessage.from(msg.getContent()));
                    }
                }
                case "TOOL" -> {
                    if (msg.getToolResult() != null) {
                        messages.add(new ToolExecutionResultMessage(
                                msg.getToolName(), // using tool name as id for simplicity
                                msg.getToolName(),
                                msg.getToolResult()
                        ));
                    }
                }
            }
        }

        // Current user input
        messages.add(UserMessage.from(userInput));

        return messages;
    }

    private String executeAgentLoop(Long sessionId, List<ChatMessage> messages, List<ToolSpecification> toolSpecs) {
        int iterations = 0;
        int maxIterations = agentProperties.getMaxToolIterations();

        while (iterations < maxIterations) {
            Response<AiMessage> response = chatModel.generate(messages, toolSpecs);
            AiMessage aiMessage = response.content();

            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text() != null ? aiMessage.text() : "处理完成";
            }

            // Save the AI message with tool calls
            messages.add(aiMessage);
            saveToolCallMessages(sessionId, aiMessage.toolExecutionRequests(), null);

            // Execute each tool
            for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                try {
                    Tool tool = toolRegistry.getTool(request.name());
                    if (tool == null) {
                        String error = "工具 '" + request.name() + "' 不存在";
                        messages.add(new ToolExecutionResultMessage(request.id(), request.name(), error));
                        saveToolCallMessages(sessionId, null, Map.of(
                                "toolCallId", request.id(),
                                "toolName", request.name(),
                                "result", error
                        ));
                        continue;
                    }

                    Map<String, Object> args = objectMapper.readValue(
                            request.arguments(), new TypeReference<>() {});
                    ToolResult result = tool.execute(args);

                    String resultStr = result.isSuccess()
                            ? objectMapper.writeValueAsString(result.getData())
                            : "错误: " + result.getError();

                    messages.add(new ToolExecutionResultMessage(request.id(), request.name(), resultStr));
                    saveToolCallMessages(sessionId, null, Map.of(
                            "toolCallId", request.id(),
                            "toolName", request.name(),
                            "result", resultStr
                    ));
                } catch (Exception e) {
                    log.error("Tool execution error: {}", e.getMessage());
                    messages.add(new ToolExecutionResultMessage(request.id(), request.name(),
                            "执行异常: " + e.getMessage()));
                }
            }

            iterations++;
        }

        // If we hit max iterations, get a final response
        Response<AiMessage> finalResponse = chatModel.generate(messages, toolSpecs);
        return finalResponse.content().text() != null
                ? finalResponse.content().text()
                : "已达最大工具调用次数，请简化问题后重试。";
    }

    private void saveToolCallMessages(Long sessionId,
                                       List<ToolExecutionRequest> requests,
                                       Map<String, Object> toolResult) {
        if (requests != null) {
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
        if (toolResult != null && sessionId != null) {
            try {
                AgentMessage msg = new AgentMessage();
                msg.setSessionId(sessionId);
                msg.setRole("TOOL");
                msg.setToolName((String) toolResult.get("toolName"));
                msg.setToolResult((String) toolResult.get("result"));
                sessionService.saveMessage(msg);
            } catch (Exception e) {
                log.error("Failed to save tool result message", e);
            }
        }
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
```

- [ ] **Step 1: Create AgentSessionService.java**
- [ ] **Step 2: Create AgentOrchestratorService.java**
- [ ] **Step 3: Commit**

```
git add knowledge-service/src/main/java/com/knowledge/agent/service/
git commit -m "feat: add agent session service and orchestration service"
```

---

### Task 10: Agent REST API (knowledge-api)

**Files:** Create 5 files:

**`knowledge-api/src/main/java/com/knowledge/agent/dto/AgentChatRequest.java`**
```java
package com.knowledge.agent.dto;

import lombok.Data;

@Data
public class AgentChatRequest {
    private Long sessionId;
    private String question;
}
```

**`knowledge-api/src/main/java/com/knowledge/agent/dto/AgentSessionDTO.java`**
```java
package com.knowledge.agent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentSessionDTO {
    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**`knowledge-api/src/main/java/com/knowledge/agent/dto/AgentMessageDTO.java`**
```java
package com.knowledge.agent.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentMessageDTO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String toolName;
    private String toolArgs;
    private String toolResult;
    private LocalDateTime createTime;
}
```

**`knowledge-api/src/main/java/com/knowledge/agent/dto/ToolInfoDTO.java`**
```java
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
```

**`knowledge-api/src/main/java/com/knowledge/agent/controller/AgentController.java`**

Create session, list sessions, delete session, ask (blocking), tool list. Follows existing ChatController patterns:

```java
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
import com.knowledge.agent.tool.ToolParameter;
import com.knowledge.agent.tool.ToolRegistry;
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
import java.util.stream.Collectors;

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
        return ResponseEntity.ok(sessions.stream().map(this::toSessionDTO).toList());
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
        return ResponseEntity.ok(messages.stream().map(this::toMessageDTO).toList());
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
                }).toList());
        return dto;
    }
}
```

- [ ] **Step 1: Create AgentChatRequest.java DTO**
- [ ] **Step 2: Create AgentSessionDTO.java**
- [ ] **Step 3: Create AgentMessageDTO.java**
- [ ] **Step 4: Create ToolInfoDTO.java**
- [ ] **Step 5: Create AgentController.java**
- [ ] **Step 6: Commit**

```
git add knowledge-api/src/main/java/com/knowledge/agent/
git commit -m "feat: add agent REST API"
```

---

### Task 11: Update schema.sql and application.yml

**Modify:** `knowledge-api/src/main/resources/db/schema.sql` — append at end:

```sql
-- Agent 会话表
CREATE TABLE IF NOT EXISTS agent_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '会话标题',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

-- Agent 消息表
CREATE TABLE IF NOT EXISTS agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/ASSISTANT/TOOL',
    content TEXT COMMENT '消息内容',
    tool_name VARCHAR(100) COMMENT '工具名称',
    tool_args TEXT COMMENT '工具参数(JSON)',
    tool_result TEXT COMMENT '工具执行结果(JSON)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent消息表';
```

**Modify:** `knowledge-api/src/main/resources/application.yml` — append:

```yaml
# Agent配置
agent:
  file-base-path: ./agent-files
  max-tool-iterations: 5
  max-history-messages: 20
```

- [ ] **Step 1: Append agent tables to schema.sql**
- [ ] **Step 2: Add agent config to application.yml**
- [ ] **Step 3: Commit**

```
git add knowledge-api/src/main/resources/db/schema.sql
git commit -m "feat: add agent tables to schema"
git add knowledge-api/src/main/resources/application.yml
git commit -m "feat: add agent config to application.yml"
```

---

### Task 12: Verify build and integration test

- [ ] **Step 1: Build the project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Fix any compilation errors** if present

- [ ] **Step 3: Start the application**

Run: `./gradlew :knowledge-api:bootRun --args='--spring.profiles.active=local'`
Expected: Application starts on port 8080

- [ ] **Step 4: Verify REST endpoints** with curl:

```bash
# Create session
curl -s -X POST http://localhost:8080/api/agent/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"test"}' | jq .

# List tools
curl -s http://localhost:8080/api/agent/tools | jq .

# List sessions
curl -s http://localhost:8080/api/agent/sessions | jq .

# Ask a question (calculator)
curl -s -X POST http://localhost:8080/api/agent/ask \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"question":"计算 25 * 4 + 100 等于多少？"}' | jq .

# Ask a question (weather)
curl -s -X POST http://localhost:8080/api/agent/ask \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"question":"北京今天天气怎么样？"}' | jq .
```

- [ ] **Step 5: Commit final build verification** (if any fixes were needed)
- [ ] **Step 6: Commit any remaining changes**

```
git add -A
git commit -m "chore: finalize tool-calling agent module"
```
