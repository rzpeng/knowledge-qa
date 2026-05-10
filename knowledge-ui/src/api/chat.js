import api from './request'

export const chatApi = {
  getSessions() {
    return api.get('/chat/sessions')
  },

  createSession(title = '新对话') {
    return api.post('/chat/sessions', { title })
  },

  getMessages(sessionId) {
    return api.get(`/chat/sessions/${sessionId}/messages`)
  },

  deleteSession(sessionId) {
    return api.delete(`/chat/sessions/${sessionId}`)
  },

  ask(sessionId, question) {
    return api.post('/chat/ask', { sessionId, question })
  }
}
