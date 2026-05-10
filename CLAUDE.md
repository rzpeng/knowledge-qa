# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A RAG (Retrieval-Augmented Generation) personal knowledge base Q&A system. Users upload documents, which are parsed, chunked, embedded, and stored in Milvus vector DB. Users can then ask questions and the system retrieves relevant chunks as context for the LLM to answer.

## Tech Stack

- **Backend**: Spring Boot 3.2.5, JDK 17, Gradle 9.5
- **RAG Framework**: LangChain4j 0.34.0
- **AI Models**: ZhipuAI GLM-4.5-AIR (chat) + embedding-3 (vectors), DeepSeek chat via OpenAI-compatible API
- **Vector DB**: Milvus 2.4.x (1024-dim, IVF_FLAT + COSINE)
- **Database**: MySQL 8.0 + MyBatis-Plus 3.5.9
- **Cache**: Redis + StringRedisTemplate
- **Frontend**: Vue 3.4 + Vite 5 + Element Plus + Pinia + Vue Router

## Project Structure

```
knowledge-qa/
├── knowledge-api/          # Entry point, REST controllers, application config
│   ├── controller/         # DocumentController, ChatController
│   ├── dto/                # ChatRequest, ChatSessionDTO, ChatMessageDTO
│   └── config/             # WebConfig (CORS)
├── knowledge-service/      # Business logic, RAG pipeline, AI integrations
│   ├── service/            # ChatService, KnowledgeDocumentService, DocumentProcessService, DocumentParserService
│   ├── config/             # ZhipuAiConfig, DeepSeekAiConfig, MilvusConfig, RagProperties, FileProperties, AsyncConfig
│   └── rag/                # MilvusVectorStore
├── knowledge-core/         # Entities, Enums, DTOs, MyBatis-Plus Mappers
│   ├── entity/             # KnowledgeDocument, KnowledgeChunk, ChatSession, ChatMessage
│   ├── enum/               # DocumentStatus, FileType, MessageRole
│   ├── mapper/             # MyBatis-Plus mapper interfaces
│   └── dto/                # DocumentDTO
└── knowledge-ui/           # Vue 3 frontend
    └── src/
        ├── views/          # ChatView.vue, DocumentsView.vue
        ├── api/            # chat.js, document.js, request.js (axios)
        └── router/         # index.js
```

## Key Architecture Flows

### Document Ingestion
`DocumentController.upload()` → `KnowledgeDocumentService.upload()` (save file to disk + DB) → `DocumentProcessService.processDocumentAsync()` (async: parse → chunk → embed via ZhipuAI → store vectors in Milvus → save chunks to MySQL)

### Chat Q&A
`ChatController.ask()` or SSE `/chat/stream` → `ChatService.chat()` (save question → embed question → Milvus similarity search → build RAG prompt with context + history → LLM generate → save answer)

## Configuration Profiles

- `application.yml` — full config with API keys, Milvus, RAG settings (default)
- `application-dev.yml` — port + datasource + redis only
- `application-local.yml` — port + datasource + redis + mybatis-plus
- `application-minimal.yml` — excludes datasource/redis autoconfig

## Important: Two AI Chat Models

The project has **two** chat model beans. `DeepSeekAiConfig.deepseekChatModel()` is marked `@Primary`, so it is the default. `ZhipuAiConfig.chatModel()` is used only when explicitly `@Qualifier`'d. The embedding model (`@Primary`) always comes from ZhipuAI.

## Common Commands

```bash
# Start backend (default profile)
./gradlew :knowledge-api:bootRun

# Start backend with dev profile
./gradlew :knowledge-api:bootRun --args='--spring.profiles.active=dev'

# Full build
./gradlew build

# Run tests (JUnit 5)
./gradlew test

# Start frontend
cd knowledge-ui && npm install && npm run dev
```

### Infrastructure Prerequisites
- Docker Milvus: `docker run -d --name milvus-standalone -p 19530:19530 -p 9091:9091 milvusdb/milvus:v2.4.0-latest`
- MySQL: create database `knowledge_qa` (utf8mb4), run `schema.sql`
- Redis: `redis-server`

## API Endpoints

### Documents
- `POST /api/documents/upload` — upload file (multipart)
- `GET /api/documents` — list all documents
- `GET /api/documents/{id}` — get document detail
- `PUT /api/documents/{id}` — update document
- `DELETE /api/documents/{id}` — delete document and related vectors

### Chat
- `POST /api/chat/sessions` — create session
- `GET /api/chat/sessions` — list sessions
- `GET /api/chat/sessions/{id}/messages` — get history
- `DELETE /api/chat/sessions/{id}` — delete session
- `POST /api/chat/ask` — ask question (blocking)
- `GET /api/chat/stream?sessionId=&question=` — SSE streaming

## Code Conventions

- Use Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
- Controller → Service → Mapper layered architecture
- DTOs for API response, entities for DB mapping (MyBatis-Plus `@TableName`)
- Enum classes for fixed-status fields (DocumentStatus, FileType, MessageRole)
- Configuration via `@ConfigurationProperties` with `@Component`
