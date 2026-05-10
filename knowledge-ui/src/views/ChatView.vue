<template>
  <div class="chat-container">
    <div class="session-sidebar">
      <div class="session-header">
        <span>对话列表</span>
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
          <el-icon><ChatDotRound /></el-icon>
          <span class="session-title">{{ session.title }}</span>
          <el-button
            type="danger"
            size="small"
            text
            @click.stop="deleteSession(session.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
    <div class="chat-main">
      <div class="message-list" ref="messageListRef">
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon :size="60"><ChatDotRound /></el-icon>
          <p>开始与知识库对话吧</p>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message', msg.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="32">我</el-avatar>
            <el-avatar v-else :size="32" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>
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
          placeholder="输入你的问题..."
          @keydown.enter.ctrl="sendMessage"
          :disabled="!currentSession || loading"
        />
        <el-button
          type="primary"
          @click="sendMessage"
          :loading="loading"
          :disabled="!inputText.trim() || !currentSession"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { chatApi } from '@/api/chat'
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
    sessions.value = await chatApi.getSessions()
    if (sessions.value.length > 0 && !currentSession.value) {
      await selectSession(sessions.value[0])
    }
  } catch (e) {
    console.error('Failed to load sessions', e)
  }
}

const createNewSession = async () => {
  try {
    const session = await chatApi.createSession('新对话')
    sessions.value.unshift(session)
    await selectSession(session)
  } catch (e) {
    ElMessage.error('创建对话失败')
  }
}

const selectSession = async (session) => {
  currentSession.value = session
  try {
    messages.value = await chatApi.getMessages(session.id)
    scrollToBottom()
  } catch (e) {
    console.error('Failed to load messages', e)
  }
}

const deleteSession = async (sessionId) => {
  try {
    await chatApi.deleteSession(sessionId)
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
      console.log('发送消息:', { sessionId: currentSession.value.id, question })
      const result = await chatApi.ask(currentSession.value.id, question)
      console.log('收到回复:', result)
      messages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: result.answer
      })
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
  return marked(content)
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
.chat-container {
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
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
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
