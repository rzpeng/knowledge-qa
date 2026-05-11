import api from './request'

export function getRoleList() {
  return api.get('/system/role')
}

export function getRole(id) {
  return api.get(`/system/role/${id}`)
}

export function saveRole(data) {
  return api.post('/system/role', data)
}

export function updateRole(data) {
  return api.put('/system/role', data)
}

export function deleteRole(id) {
  return api.delete(`/system/role/${id}`)
}

export function assignRoleMenus(roleId, menuIds) {
  return api.post(`/system/role/${roleId}/menus`, menuIds)
}

export function getRoleMenuIds(roleId) {
  return api.get(`/system/role/${roleId}/menus`)
}

export function assignRoleDepts(roleId, deptIds) {
  return api.post(`/system/role/${roleId}/depts`, deptIds)
}

export function getRoleDeptIds(roleId) {
  return api.get(`/system/role/${roleId}/depts`)
}

export function assignRoleRegions(roleId, regionIds) {
  return api.post(`/system/role/${roleId}/regions`, regionIds)
}

export function getRoleRegionIds(roleId) {
  return api.get(`/system/role/${roleId}/regions`)
}
