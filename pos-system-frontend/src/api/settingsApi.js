import { apiRequest, uploadFile } from './client'

export function getSettings() {
  return apiRequest('/settings')
}

export function updateStoreSettings(payload) {
  return apiRequest('/settings/store', {
    method: 'PUT',
    body: payload,
  })
}

export function uploadStoreLogo(file) {
  return uploadFile('/settings/store/logo', file)
}

export function updateReceiptSettings(payload) {
  return apiRequest('/settings/receipt', {
    method: 'PUT',
    body: payload,
  })
}

export function changeAdminPassword(payload) {
  return apiRequest('/settings/password', {
    method: 'PATCH',
    body: payload,
  })
}
