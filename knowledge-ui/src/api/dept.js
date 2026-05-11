import api from './request'

export function getDeptTree() {
  return api.get('/api/system/dept/tree')
}

export function getDeptList() {
  return api.get('/api/system/dept/list')
}

export function getDept(id) {
  return api.get(`/api/system/dept/${id}`)
}

export function saveDept(data) {
  return api.post('/api/system/dept', data)
}

export function updateDept(data) {
  return api.put('/api/system/dept', data)
}

export function deleteDept(id) {
  return api.delete(`/api/system/dept/${id}`)
}
