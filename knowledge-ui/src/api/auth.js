import api from './request'

export function login(data) {
  return api.post('/auth/login', data)
}

export function getUserInfo() {
  return api.get('/auth/userinfo')
}

export function getUserMenus() {
  return api.get('/auth/menus')
}
