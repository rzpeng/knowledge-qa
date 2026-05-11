import api from './request'

export function getAccountPage(page, size) {
  return api.get('/system/account', { params: { page, size } })
}

export function getAccount(id) {
  return api.get(`/system/account/${id}`)
}

export function getAccountsByUser(userId) {
  return api.get(`/system/account/by-user/${userId}`)
}

export function saveAccount(data) {
  return api.post('/system/account', data)
}

export function updateAccount(data) {
  return api.put('/system/account', data)
}

export function deleteAccount(id) {
  return api.delete(`/system/account/${id}`)
}
