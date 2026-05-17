<template>
  <div class="agent-container">
    <div class="session-sidebar">
      <div class="session-header">
        <span>智能助手</span>
        <el-button type="primary" size="small" @click="createNewSession">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          :class="['session-item', { active: currentSession?.id === session.id }]"
          @click="selectSession(session)"
        >
          <el-icon><MagicStick /></el-icon>
          <span class="session-title">{{ session.title }}</span>
          <el-button type="danger" size="small" text @click.stop="deleteSession(session.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
    <div class="chat-main">
      <div class="message-list" ref="messageListRef">
        <div v-if="displayMessages.length === 0 && !loading" class="empty-state">
          <el-icon :size="60"><MagicStick /></el-icon>
          <p>智能助手可以帮你查询信息、执行计算、操作文件等</p>
        </div>
        <div
          v-for="item in displayMessages"
          :key="item.id"
          :class="['message-row', item.type === 'user' ? 'user-row' : 'ai-row']"
        >
          <div class="message-sender">
            <el-avatar v-if="item.type === 'user'" :size="36" class="user-avatar">我</el-avatar>
            <el-avatar v-else :size="36" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="message-body">
            <div class="sender-name">{{ item.type === 'user' ? '我' : 'AI 智能助手' }}</div>
            <div class="bubble" :class="item.type === 'user' ? 'user-bubble' : 'ai-bubble'">
              <div v-if="item.tools && item.tools.length > 0" class="tool-section">
                <el-collapse accordion>
                  <el-collapse-item>
                    <template #title>
                      <span class="tool-call-count">🛠 调用了 {{ item.tools.length }} 个工具</span>
                    </template>
                    <div v-for="(tool, idx) in item.tools" :key="idx" class="tool-detail">
                      <div class="tool-name">{{ tool.name }}</div>
                      <div class="tool-args">
                        <span class="detail-label">参数:</span>
                        <pre>{{ formatJson(tool.arguments) }}</pre>
                      </div>
                      <div v-if="tool.result" class="tool-result">
                        <span class="detail-label">结果:</span>
                        <pre>{{ tool.result }}</pre>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
              <div class="message-text" v-html="formatMessage(item.content)"></div>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message-row ai-row">
          <div class="message-sender">
            <el-avatar :size="36" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="message-body">
            <div class="sender-name">AI 智能助手</div>
            <div class="bubble ai-bubble">
              <div class="loading-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入你的需求..."
          @keydown.enter.ctrl="sendMessage"
          :disabled="!currentSession || loading"
        />
        <el-button type="primary" @click="sendMessage" :loading="loading"
          :disabled="!inputText.trim() || !currentSession">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { agentApi } from '@/api/agent'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messageListRef = ref(null)

const displayMessages = computed(() => {
  const result = []
  let pendingToolCalls = []

  for (const msg of messages.value) {
    if (msg.role === 'USER') {
      pendingToolCalls = []
      result.push({ type: 'user', content: msg.content || '', id: msg.id })
    } else if (msg.role === 'ASSISTANT') {
      if (msg.toolArgs) {
        try {
          const tools = JSON.parse(msg.toolArgs)
          if (Array.isArray(tools)) {
            pendingToolCalls.push(...tools)
          }
        } catch { /* ignore parse errors */ }
      }
      if (msg.content) {
        result.push({
          type: 'assistant',
          content: msg.content,
          tools: pendingToolCalls.length > 0 ? [...pendingToolCalls] : null,
          id: msg.id
        })
        pendingToolCalls = []
      }
    } else if (msg.role === 'TOOL' && pendingToolCalls.length > 0) {
      // Attach tool result to the pending tool call that matches
      const tc = pendingToolCalls.find(t => t.id === msg.toolCallId || t.name === msg.toolName)
      if (tc) tc.result = msg.toolResult
    }
  }
  return result
})

onMounted(async () => {
  await loadSessions()
})

const loadSessions = async () => {
  try {
    sessions.value = await agentApi.getSessions()
    if (sessions.value.length > 0 && !currentSession.value) {
      await selectSession(sessions.value[0])
    }
  } catch (e) {
    console.error('Failed to load sessions', e)
  }
}

const createNewSession = async () => {
  try {
    const session = await agentApi.createSession('新对话')
    sessions.value.unshift(session)
    await selectSession(session)
  } catch (e) {
    ElMessage.error('创建对话失败')
  }
}

const selectSession = async (session) => {
  currentSession.value = session
  try {
    messages.value = await agentApi.getMessages(session.id)
    scrollToBottom()
  } catch (e) {
    console.error('Failed to load messages', e)
  }
}

const deleteSession = async (sessionId) => {
  try {
    await agentApi.deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    if (currentSession.value?.id === sessionId) {
      currentSession.value = sessions.value[0] || null
      if (currentSession.value) {
        await selectSession(currentSession.value)
      } else {
        messages.value = []
      }
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim() || !currentSession.value || loading.value) return

  const question = inputText.value.trim()
  inputText.value = ''
  loading.value = true

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: question
  })
  scrollToBottom()

  try {
    const result = await agentApi.ask(currentSession.value.id, question)
    const history = await agentApi.getMessages(currentSession.value.id)
    messages.value = history
    scrollToBottom()
  } catch (e) {
    console.error('发送失败:', e)
    const errorMsg = e.response?.data?.message || e.message || '发送失败，请重试'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

const formatMessage = (content) => {
  if (!content) return ''
  return marked(content)
}

const formatJson = (val) => {
  if (!val) return ''
  try {
    const obj = typeof val === 'string' ? JSON.parse(val) : val
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(val)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.agent-container {
  display: flex;
  height: 100%;
}

.session-sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.session-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Chat main area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 40px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 12px;
}

/* Message layout: left for AI, right for user */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  max-width: 85%;
}

.user-row {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-sender {
  flex-shrink: 0;
  margin-top: 20px;
}

.user-avatar {
  background: #409eff;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-row .message-body {
  align-items: flex-end;
}

.sender-name {
  font-size: 12px;
  color: #909399;
  padding: 0 4px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.user-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* Tool calls inside AI bubble */
.tool-section {
  margin-bottom: 8px;
}

.tool-section :deep(.el-collapse-item__header) {
  font-size: 12px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  height: 32px;
}

.tool-call-count {
  font-weight: 500;
  color: #606266;
}

.tool-detail {
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tool-detail:last-child {
  border-bottom: none;
}

.tool-name {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}

.detail-label {
  font-size: 11px;
  color: #909399;
  display: block;
  margin-bottom: 2px;
}

.tool-detail pre {
  background: #fafafa;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 11px;
  overflow-x: auto;
  margin: 0;
  max-height: 120px;
  overflow-y: auto;
}

/* Loading */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Input area */
.input-area {
  padding: 16px;
  background: #fff;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  border-top: 1px solid #e4e7ed;
}

.input-area .el-textarea {
  flex: 1;
}
</style>
