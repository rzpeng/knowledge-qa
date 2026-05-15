# Tool-Calling Agent 智能助手模块设计

## 概述

在现有知识库 Q&A 系统上新增一个独立的 Tool-Calling Agent 模块，支持用户通过自然语言调用多个工具（天气查询、计算器、数据库查询、文件操作等），实现"帮我查北京天气，再算下明天比今天高多少度"这类多工具串联场景。

## 架构

### 模块划分（不新增 Gradle 子模块）

| 层级 | 新增内容 |
|------|---------|
| `knowledge-core` | 工具接口规范 (`Tool`), 参数定义 (`ToolParameter`), 结果封装 (`ToolResult`), Agent 会话/消息实体, 枚举, Mapper |
| `knowledge-service` | 工具实现 (WeatherTool, CalculatorTool, DatabaseQueryTool, FileOperationTool), ToolRegistry, AgentOrchestratorService, Prompt 模板 |
| `knowledge-api` | AgentController, 请求/响应 DTO |

### 核心模块关系

```
用户 → AgentController → AgentOrchestratorService
                            │
                   ┌────────┴────────┐
                   │                 │
            DeepSeek LLM       ToolRegistry
            (意图识别/工具         │
             选择/参数提取)     ┌──┴──┐
                              │     │
                         Tool 接口  转换器
                         │  │  │    │
                     天气 计算 查询 文件 ToolSpecification
                    工具 器  工具 工具 → LangChain4j
```

## 工具接口规范

### Tool 接口

```java
public interface Tool {
    String name();                          // 唯一标识，如 "weather_query"
    String description();                   // 功能描述，供 LLM 选择
    List<ToolParameter> parameters();       // 参数定义
    ToolResult execute(Map<String, Object> args);  // 执行逻辑
}
```

### ToolParameter

```java
public class ToolParameter {
    String name;           // 参数名
    String description;    // 参数描述
    ParamType type;        // STRING / NUMBER / BOOLEAN
    boolean required;      // 是否必填
}
```

### ToolResult

```java
public class ToolResult {
    boolean success;
    Object data;       // 执行结果
    String error;      // 错误信息
}
```

### 转换器

`FunctionToolSpecConverter` 负责将自定义 `Tool` 列表转换为 LangChain4j 的 `ToolSpecification` 列表，通过标准 Function Calling 协议发送给 DeepSeek 模型。这样既保持了自定义接口的清晰度，又复用了现有模型通道。

## 基础工具（4 个）

### 1. WeatherTool — 天气查询
- **参数**: `city`(string, 必填), `date`(string, 可选，格式 YYYY-MM-DD)
- **实现**: 调用 Open-Meteo API（https://api.open-meteo.com，无需 API Key），解析返回数据
- **输出**: {temperature, condition, humidity, wind}

### 2. CalculatorTool — 计算器
- **参数**: `expression`(string, 必填)，如 "25 + 10 * 2 / (8-3)"
- **实现**: Java ScriptEngine 或自定义表达式解析器本地执行
- **输出**: {result: number}

### 3. DatabaseQueryTool — 数据库查询
- **参数**: `sql`(string, 必填)
- **实现**: 强制校验为 SELECT 只读语句，通过 MyBatis-Plus 执行，限制返回行数
- **输出**: {columns: [], rows: [[]]}

### 4. FileOperationTool — 文件操作
- **参数**: `operation`(string, 必填: read/write/list), `path`(string, 必填), `content`(string, write 时使用)
- **实现**: 在 `${file.agent-base-path}`（默认 `./agent-files`）沙箱目录内操作，禁止路径穿越（路径规范化后校验前缀）
- **输出**: 文件内容或操作结果

## 工具注册中心（ToolRegistry）

```java
public class ToolRegistry {
    void register(Tool tool);          // 注册工具
    void unregister(String name);      // 卸载工具
    Tool getTool(String name);         // 按名称获取
    List<Tool> getAllTools();          // 发现所有已注册工具
    List<ToolSpecification> getSpecifications(); // 获取所有工具的描述
}
```

- 工具通过 `@PostConstruct` 或手动调用 `register()` 注册
- 支持运行期动态注册和卸载
- 注册时校验工具名称唯一性

## Agent 编排（AgentOrchestratorService）

### 处理流程

```
1. 接收用户消息
2. 加载最近 N 轮对话历史
3. 从 ToolRegistry 获取工具列表 → 转为 ToolSpecification
4. 调用 DeepSeek LLM (含完整 Prompt + 工具描述)
5. LLM 返回:
   a) 文本回复 → 直接返回
   b) 工具调用请求 → 进入步骤 6
6. ToolRegistry 解析工具名 → 执行工具
7. 工具结果返回 LLM 做下一轮生成
8. 重复 5-7，直到 LLM 返回文本或达上限 (最多 5 轮)
9. 保存消息到数据库 → 返回最终回复
```

### Prompt 模板

```
你是一个智能助手，可以通过调用工具来回答用户的问题。
当前可用的工具有：

{tool_descriptions}

对话历史：
{chat_history}

用户：{user_input}
请根据用户需求选择合适的工具。如果需要多个工具协作，请依次调用。
```

### 多工具串联示例

```
用户: "查一下北京今天和明天的温度，计算温差"

→ LLM 第1轮: 调用 weather_query(city="北京", date="2026-05-15")
→ 返回: {temp: 28, condition: "晴"}
→ LLM 第2轮: 调用 weather_query(city="北京", date="2026-05-16")
→ 返回: {temp: 32, condition: "多云"}
→ LLM 第3轮: 调用 calculator(expression="32-28")
→ 返回: {result: 4}
→ LLM 最终: "北京明天32°C比今天28°C高4°C"
```

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| 工具不存在 | LLM 告知用户该功能不可用 |
| 参数缺失/类型错误 | 返回 LLM 重新提取参数再试 |
| 工具执行异常 | 重试 1 次 → 失败后由 LLM 告知用户 |
| LLM 超时 | 返回超时提示 |
| 工具调用死循环 | 单轮上限 5 次，超限终止 |
| 数据库注入 | 强制 SELECT 只读校验 + MyBatis 参数化查询 |
| 路径穿越 | 沙箱路径规范化校验 |

## 数据模型（复用现有实体模式）

### AgentSession（新建表）
```sql
CREATE TABLE agent_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME
);
```

### AgentMessage（新建表）
```sql
CREATE TABLE agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,    -- USER / ASSISTANT / TOOL
    content TEXT,
    tool_name VARCHAR(100),
    tool_args TEXT,
    tool_result TEXT,
    create_time DATETIME
);
```

## REST API

```
POST   /api/agent/sessions          — 创建 Agent 会话
GET    /api/agent/sessions          — 会话列表
DELETE /api/agent/sessions/{id}     — 删除会话
POST   /api/agent/ask               — 发送消息 {sessionId, question}
GET    /api/agent/stream?sessionId=&question= — SSE 流式
GET    /api/agent/tools             — 查看已注册工具列表
```

## 与现有模块的关系

- **独立新入口**: 使用 `/api/agent/*` 路径，与 `/api/chat/*` 隔离
- **独立会话管理**: AgentSession / AgentMessage 表与现有 ChatSession / ChatMessage 独立
- **共享基础设施**: 复用 DeepSeek 模型、数据库连接、Redis 缓存
- **松耦合**: 不修改现有 ChatService / ChatController 的任何代码
