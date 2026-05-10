# 知识库问答系统

基于 RAG (Retrieval-Augmented Generation) 技术的个人知识库问答系统。

## 技术栈

### 后端
- Spring Boot 3.2.x + JDK 17
- LangChain4j 0.35.0 (集成智谱AI)
- Milvus 2.4.x (向量数据库)
- MySQL 8.0 + MyBatis-Plus
- Redis (会话缓存)
- Gradle 9.5.0

### 前端
- Vue 3.4 + Vite 5
- Element Plus 2.5
- Pinia + Vue Router

## 快速开始

### 1. 环境准备

#### 启动 Milvus (Docker)
```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.4.0-latest
```

#### 创建 MySQL 数据库
```bash
mysql -u root -p
```
```sql
CREATE DATABASE knowledge_qa DEFAULT CHARACTER SET utf8mb4;
```

#### 启动 Redis
```bash
redis-server
```

### 2. 配置

编辑 `knowledge-api/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_qa
    username: root
    password: your_password

zhipu:
  api-key: your_zhipu_api_key
```

### 3. 初始化数据库

执行 `knowledge-api/src/main/resources/db/schema.sql` 中的SQL脚本。

### 4. 启动后端

```bash
cd knowledge-qa
./gradlew :knowledge-api:bootRun
```

Windows:
```cmd
gradlew.bat :knowledge-api:bootRun
```

### 5. 启动前端

```bash
cd knowledge-qa/knowledge-ui
npm install
npm run dev
```

### 6. 访问应用

打开浏览器访问: http://localhost:5173

## 功能特性

- 文档上传与管理 (支持 TXT, Markdown, PDF, DOCX)
- 自动文档解析与切片
- 向量化存储 (Milvus)
- 基于知识库的智能问答
- 会话历史管理

## 项目结构

```
knowledge-qa/
├── knowledge-api/          # REST API模块
├── knowledge-service/      # 业务服务模块
├── knowledge-core/         # 核心模块(实体、枚举)
├── knowledge-ui/           # Vue前端
├── build.gradle
└── settings.gradle
```

## API 接口

### 文档管理
- `POST /api/documents/upload` - 上传文档
- `GET /api/documents` - 文档列表
- `DELETE /api/documents/{id}` - 删除文档

### 问答
- `POST /api/chat/sessions` - 创建会话
- `GET /api/chat/sessions` - 会话列表
- `GET /api/chat/sessions/{id}/messages` - 历史消息
- `POST /api/chat/ask` - 发起问答

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `rag.chunk-size` | 文档切片大小 | 500 |
| `rag.chunk-overlap` | 切片重叠字数 | 50 |
| `rag.top-k` | 检索返回数量 | 5 |
| `zhipu.model` | 智谱AI模型 | glm-4-flash |
| `zhipu.embedding-model` | Embedding模型 | embedding-3 |
