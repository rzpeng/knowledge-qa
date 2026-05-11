import api from './request'

export function getRegionList() {
  return api.get('/api/system/region')
}

export function getRegion(id) {
  return api.get(`/api/system/region/${id}`)
}

export function saveRegion(data) {
  return api.post('/api/system/region', data)
}

export function updateRegion(data) {
  return api.put('/api/system/region', data)
}

export function deleteRegion(id) {
  return api.delete(`/api/system/region/${id}`)
}
