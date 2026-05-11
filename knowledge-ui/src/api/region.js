import api from './request'

export function getRegionList() {
  return api.get('/system/region')
}

export function getRegion(id) {
  return api.get(`/system/region/${id}`)
}

export function saveRegion(data) {
  return api.post('/system/region', data)
}

export function updateRegion(data) {
  return api.put('/system/region', data)
}

export function deleteRegion(id) {
  return api.delete(`/system/region/${id}`)
}
