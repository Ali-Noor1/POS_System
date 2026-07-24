import { apiRequest } from './client'

export function completeSale(payload) {
  return apiRequest('/sales/complete', {
    method: 'POST',
    body: payload,
  })
}

export function getSalesHistory() {
  return apiRequest('/sales/history')
}

export function getSaleDetails(saleId) {
  return apiRequest(`/sales/${saleId}`)
}

export function cancelSale(saleId, cancellationReason) {
  return apiRequest(`/sales/${saleId}/cancel`, {
    method: 'PATCH',
    body: { cancellationReason },
  })
}
