import { apiRequest } from './client'

export function getCustomers() {
  return apiRequest('/customers')
}

export function createCustomer(payload) {
  return apiRequest('/customers', {
    method: 'POST',
    body: payload,
  })
}

export function updateCustomer(id, payload) {
  return apiRequest(`/customers/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateCustomerStatus(id, status) {
  return apiRequest(`/customers/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function searchCustomers(query) {
  return apiRequest(`/customers/search?query=${encodeURIComponent(query)}`)
}

export function createQuickCustomer(payload) {
  return apiRequest('/customers/quick-create', {
    method: 'POST',
    body: payload,
  })
}
