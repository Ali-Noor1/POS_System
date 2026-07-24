import { apiRequest } from './client'

export function getCategories() {
  return apiRequest('/categories')
}

export function createCategory(payload) {
  return apiRequest('/categories', {
    method: 'POST',
    body: payload,
  })
}

export function updateCategory(id, payload) {
  return apiRequest(`/categories/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateCategoryStatus(id, status) {
  return apiRequest(`/categories/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}
