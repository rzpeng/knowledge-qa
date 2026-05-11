import api from './request'

export function getUserPage(page, size) {
  return api.get('/api/system/user', { params: { page, size } })
}

export function getUser(id) {
  return api.get(`/api/system/user/${id}`)
}

export function saveUser(data) {
  return api.post('/api/system/user', data)
}

export function updateUser(data) {
  return api.put('/api/system/user', data)
}

export function deleteUser(id) {
  return api.delete(`/api/system/user/${id}`)
}

export function assignUserDepts(userId, deptIds, leaderDeptIds) {
  return api.post(`/api/system/user/${userId}/depts`, { deptIds, leaderDeptIds })
}

export function getUserDeptIds(userId) {
  return api.get(`/api/system/user/${userId}/depts`)
}

export function assignUserRegions(userId, regionIds) {
  return api.post(`/api/system/user/${userId}/regions`, regionIds)
}

export function getUserRegionIds(userId) {
  return api.get(`/api/system/user/${userId}/regions`)
}

export function assignUserRoles(userId, roleIds) {
  return api.post(`/api/system/user/${userId}/roles`, roleIds)
}

export function getUserRoleIds(userId) {
  return api.get(`/api/system/user/${userId}/roles`)
}
