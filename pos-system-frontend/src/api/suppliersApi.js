import { apiRequest } from './client'

export function getSuppliers() {
  return apiRequest('/suppliers')
}

export function getActiveSuppliers() {
  return apiRequest('/suppliers/active')
}

export function createSupplier(payload) {
  return apiRequest('/suppliers', {
    method: 'POST',
    body: payload,
  })
}

export function updateSupplier(id, payload) {
  return apiRequest(`/suppliers/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateSupplierStatus(id, status) {
  return apiRequest(`/suppliers/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function searchSuppliers(query) {
  return apiRequest(`/suppliers/search?query=${encodeURIComponent(query)}`)
}

export function getPurchases() {
  return apiRequest('/purchases')
}

export function getPurchasesBySupplier(supplierId) {
  return apiRequest(`/purchases/supplier/${supplierId}`)
}

export function createPurchase(payload) {
  return apiRequest('/purchases', {
    method: 'POST',
    body: payload,
  })
}

export function getSupplierPayments() {
  return apiRequest('/supplier-payments')
}

export function getSupplierPaymentsBySupplier(supplierId) {
  return apiRequest(`/supplier-payments/supplier/${supplierId}`)
}

export function createSupplierPayment(payload) {
  return apiRequest('/supplier-payments', {
    method: 'POST',
    body: payload,
  })
}
