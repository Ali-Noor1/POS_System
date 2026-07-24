import { apiRequest } from './client'

export function getCashiers() {
  return apiRequest('/admin/users/cashiers')
}

export function createCashier(payload) {
  return apiRequest('/admin/users/cashiers', {
    method: 'POST',
    body: payload,
  })
}

export function updateCashier(id, payload) {
  return apiRequest(`/admin/users/cashiers/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateCashierStatus(id, status) {
  return apiRequest(`/admin/users/cashiers/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function resetCashierPassword(id, newPassword) {
  return apiRequest(`/admin/users/cashiers/${id}/password`, {
    method: 'PATCH',
    body: { newPassword },
  })
}
