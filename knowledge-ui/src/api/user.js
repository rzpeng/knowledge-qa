import api from './request'

export function getUserPage(page, size) {
  return api.get('/system/user', { params: { page, size } })
}

export function getUser(id) {
  return api.get(`/system/user/${id}`)
}

export function saveUser(data) {
  return api.post('/system/user', data)
}

export function updateUser(data) {
  return api.put('/system/user', data)
}

export function deleteUser(id) {
  return api.delete(`/system/user/${id}`)
}

export function assignUserDepts(userId, deptIds, leaderDeptIds) {
  return api.post(`/system/user/${userId}/depts`, { deptIds, leaderDeptIds })
}

export function getUserDeptIds(userId) {
  return api.get(`/system/user/${userId}/depts`)
}

export function assignUserRegions(userId, regionIds) {
  return api.post(`/system/user/${userId}/regions`, regionIds)
}

export function getUserRegionIds(userId) {
  return api.get(`/system/user/${userId}/regions`)
}

export function assignUserRoles(userId, roleIds) {
  return api.post(`/system/user/${userId}/roles`, roleIds)
}

export function getUserRoleIds(userId) {
  return api.get(`/system/user/${userId}/roles`)
}
