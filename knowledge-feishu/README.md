# 飞书集成模块

## 概述

飞书集成模块为知识库问答系统提供飞书机器人功能，支持消息接收、发送以及知识库问答结果推送。

## 功能特性

- 🤖 飞书机器人消息接收与处理
- 📱 文本消息和卡片消息发送
- 🧠 知识库问答结果推送
- 🔗 Webhook消息处理
- ⚙️ 配置管理

## 快速开始

### 1. 配置飞书机器人

1. 在飞书开放平台创建应用
2. 获取 App ID 和 App Secret
3. 配置机器人权限
4. 获取 Bot Token 和 Webhook URL

### 2. 配置应用

编辑 `knowledge-feishu/src/main/resources/application.yml`:

```yaml
feishu:
  enabled: true
  app-id: your_app_id
  app-secret: your_app_secret
  bot-token: your_bot_token
  webhook-url: https://open.feishu.cn/open-apis/bot/v2/hook/your_webhook_hook
```

### 3. 启动服务

```bash
cd knowledge-feishu
./gradlew bootRun
```

Windows:
```cmd
gradlew.bat bootRun
```

### 4. 访问服务

服务启动后，可以通过以下端点访问：

- `POST /api/feishu/webhook` - 飞书Webhook消息接收
- `POST /api/feishu/message/text` - 发送文本消息
- `POST /api/feishu/message/card` - 发送卡片消息
- `POST /api/feishu/message/knowledge` - 发送知识库问答结果
- `GET /api/feishu/health` - 健康检查

## API 接口

### Webhook 接口

```bash
POST /api/feishu/webhook
Content-Type: application/json

{
  "msg_type": "text",
  "chat_id": "oc_xxx",
  "text": "你好",
  "open_id": "ou_xxx"
}
```

### 发送文本消息

```bash
POST /api/feishu/message/text?chat_id=oc_xxx&text=你好
```

### 发送卡片消息

```bash
POST /api/feishu/message/card?chat_id=oc_xxx
Content-Type: application/json

{
  "header": {
    "title": {
      "tag": "plain_text",
      "content": "通知"
    },
    "template": "turquoise"
  },
  "elements": [
    {
      "tag": "div",
      "fields": {
        "title": {
          "tag": "plain_text",
          "content": "这是一个卡片消息"
        }
      }
    }
  ]
}
```

### 发送知识库问答结果

```bash
POST /api/feishu/message/knowledge?chat_id=oc_xxx&question=问题&答案=答案
```

## 消息类型支持

### 文本消息

```json
{
  "msg_type": "text",
  "content": {
    "text": "这是一条文本消息"
  }
}
```

### 卡片消息

```json
{
  "msg_type": "interactive",
  "card": {
    "header": {
      "title": {
        "tag": "plain_text",
        "content": "标题"
      },
      "template": "turquoise"
    },
    "elements": [
      {
        "tag": "div",
        "fields": {
          "field1": {
            "tag": "plain_text",
            "content": "字段1内容"
          }
        }
      }
    ]
  }
}
```

## 集成示例

### 1. 在现有聊天服务中集成飞书

```java
@Service
public class ChatService {
    
    @Autowired
    private FeishuService feishuService;
    
    public ChatResponse askQuestion(String question) {
        // 调用知识库问答
        String answer = knowledgeService.ask(question);
        
        // 推送到飞书
        feishuService.sendKnowledgeAnswer(chatId, question, answer);
        
        return new ChatResponse(answer);
    }
}
```

### 2. 自定义消息处理

```java
@Service
public class CustomFeishuHandler {
    
    @Autowired
    private FeishuService feishuService;
    
    public void handleTextMessage(FeishuMessage message) {
        String text = message.getText();
        String chatId = message.getChat_id();
        
        // 处理用户输入
        if (text.contains("帮助")) {
            feishuService.sendTextMessage(chatId, "我可以帮您查询知识库内容");
        }
    }
}
```

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `feishu.enabled` | 是否启用飞书集成 | false |
| `feishu.app-id` | 飞书应用ID | - |
| `feishu.app-secret` | 飞书应用密钥 | - |
| `feishu.bot-token` | 机器人访问令牌 | - |
| `feishu.webhook-url` | Webhook URL | - |

## 故障排除

### 1. 消息发送失败

检查配置项：
- Bot Token 是否正确
- Webhook URL 是否有效
- 网络连接是否正常

### 2. Webhook 接收失败

检查：
- 服务是否正常运行
- 端口是否正确
- 飞书机器人配置是否正确

### 3. 权限问题

确保飞书应用具有以下权限：
- 发送消息权限
- 读取消息权限

## 开发指南

### 添加新的消息类型

1. 在 `FeishuMessage` 中添加新的消息类型
2. 在 `FeishuService` 中添加对应的处理方法
3. 更新 Webhook 控制器

### 自定义卡片模板

1. 在 `FeishuService` 中创建新的卡片模板方法
2. 在控制器中调用新的模板方法

## 测试

```bash
# 测试健康检查
curl http://localhost:8081/api/feishu/health

# 测试发送文本消息
curl -X POST "http://localhost:8081/api/feishu/message/text?chat_id=test&text=Hello"

# 测试发送知识库问答
curl -X POST "http://localhost:8081/api/feishu/message/knowledge?chat_id=test&question=测试问题&答案=测试答案"
```