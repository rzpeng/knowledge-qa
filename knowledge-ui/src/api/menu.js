import api from './request'

export function getMenuTree() {
  return api.get('/system/menu/tree')
}

export function getMenu(id) {
  return api.get(`/system/menu/${id}`)
}

export function saveMenu(data) {
  return api.post('/system/menu', data)
}

export function updateMenu(data) {
  return api.put('/system/menu', data)
}

export function deleteMenu(id) {
  return api.delete(`/system/menu/${id}`)
}
