import api from './request'

export const documentApi = {
  list() {
    return api.get('/documents')
  },

  get(id) {
    return api.get(`/documents/${id}`)
  },

  upload(file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },

  delete(id) {
    return api.delete(`/documents/${id}`)
  }
}
