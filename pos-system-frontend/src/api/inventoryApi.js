import { apiRequest } from './client'

export function adjustInventory(payload) {
  return apiRequest('/inventory/adjustments', {
    method: 'POST',
    body: payload,
  })
}

export function getProductInventoryTransactions(productId) {
  return apiRequest(`/inventory/products/${productId}/transactions`)
}

export function getLowStockProducts() {
  return apiRequest('/inventory/low-stock')
}
