import api from './request'

export function login(data) {
  return api.post('/api/auth/login', data)
}

export function getUserInfo() {
  return api.get('/api/auth/userinfo')
}

export function getUserMenus() {
  return api.get('/api/auth/menus')
}
