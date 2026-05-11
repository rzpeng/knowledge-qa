import api from './request'

export function getDeptTree() {
  return api.get('/system/dept/tree')
}

export function getDeptList() {
  return api.get('/system/dept/list')
}

export function getDept(id) {
  return api.get(`/system/dept/${id}`)
}

export function saveDept(data) {
  return api.post('/system/dept', data)
}

export function updateDept(data) {
  return api.put('/system/dept', data)
}

export function deleteDept(id) {
  return api.delete(`/system/dept/${id}`)
}
