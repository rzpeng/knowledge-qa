import api from './request'

export function getMenuTree() {
  return api.get('/api/system/menu/tree')
}

export function getMenu(id) {
  return api.get(`/api/system/menu/${id}`)
}

export function saveMenu(data) {
  return api.post('/api/system/menu', data)
}

export function updateMenu(data) {
  return api.put('/api/system/menu', data)
}

export function deleteMenu(id) {
  return api.delete(`/api/system/menu/${id}`)
}
