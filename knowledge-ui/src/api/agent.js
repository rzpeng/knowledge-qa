import request from './request'

export const agentApi = {
  getSessions() {
    return request.get('/agent/sessions')
  },
  createSession(title) {
    return request.post('/agent/sessions', { title })
  },
  getMessages(sessionId) {
    return request.get(`/agent/sessions/${sessionId}/messages`)
  },
  deleteSession(sessionId) {
    return request.delete(`/agent/sessions/${sessionId}`)
  },
  ask(sessionId, question) {
    return request.post('/agent/ask', { sessionId, question })
  },
  getTools() {
    return request.get('/agent/tools')
  }
}
