import api from './request'

export function getAccountPage(page, size) {
  return api.get('/api/system/account', { params: { page, size } })
}

export function getAccount(id) {
  return api.get(`/api/system/account/${id}`)
}

export function getAccountsByUser(userId) {
  return api.get(`/api/system/account/by-user/${userId}`)
}

export function saveAccount(data) {
  return api.post('/api/system/account', data)
}

export function updateAccount(data) {
  return api.put('/api/system/account', data)
}

export function deleteAccount(id) {
  return api.delete(`/api/system/account/${id}`)
}
