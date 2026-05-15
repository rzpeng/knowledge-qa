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
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon :size="60"><MagicStick /></el-icon>
          <p>智能助手可以帮你查询信息、执行计算、操作文件等</p>
        </div>
        <template v-for="msg in messages" :key="msg.id">
          <div v-if="msg.role === 'TOOL'" class="message tool">
            <div class="tool-call">
              <el-collapse accordion>
                <el-collapse-item>
                  <template #title>
                    <span class="tool-title">🛠 {{ msg.toolName }}</span>
                  </template>
                  <div class="tool-detail">
                    <div class="tool-section">
                      <span class="tool-label">参数:</span>
                      <pre>{{ formatJson(msg.toolArgs) }}</pre>
                    </div>
                    <div class="tool-section">
                      <span class="tool-label">结果:</span>
                      <pre>{{ formatJson(msg.toolResult) }}</pre>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
          <div v-else :class="['message', msg.role]">
            <div class="message-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="32">我</el-avatar>
              <el-avatar v-else :size="32" class="ai-avatar">AI</el-avatar>
            </div>
            <div class="message-content">
              <div v-if="msg.toolName" class="tool-badge">
                🛠 调用工具: {{ msg.toolName }}
              </div>
              <div class="message-text" v-html="formatMessage(msg.content || '')"></div>
            </div>
          </div>
        </template>
        <div v-if="loading" class="message assistant">
          <div class="message-avatar">
            <el-avatar :size="32" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text loading">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入你的需求，我会调用工具来帮助你..."
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
import { ref, onMounted, nextTick } from 'vue'
import { agentApi } from '@/api/agent'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'

const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const messageListRef = ref(null)

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

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
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

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message.tool {
  justify-content: center;
  margin-bottom: 4px;
}

.message-content {
  max-width: 70%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 8px;
  background: #fff;
  line-height: 1.6;
}

.message.user .message-text {
  background: #409eff;
  color: #fff;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.tool-badge {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  padding-left: 4px;
}

.tool-call {
  width: 90%;
}

.tool-call :deep(.el-collapse-item__header) {
  font-size: 13px;
  padding-left: 8px;
  background: #f0f2f5;
  border-radius: 4px;
}

.tool-title {
  font-weight: 600;
}

.tool-detail {
  padding: 8px;
}

.tool-section {
  margin-bottom: 8px;
}

.tool-label {
  font-weight: 600;
  font-size: 12px;
  color: #606266;
  display: block;
  margin-bottom: 4px;
}

.tool-detail pre {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
}

.loading {
  display: flex;
  gap: 4px;
}

.loading span {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.loading span:nth-child(1) { animation-delay: -0.32s; }
.loading span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px;
  background: #fff;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}
</style>
