import { apiRequest, uploadFile } from './client'

export function getProducts() {
  return apiRequest('/products')
}

export function createProduct(payload) {
  return apiRequest('/products', {
    method: 'POST',
    body: payload,
  })
}

export function updateProduct(id, payload) {
  return apiRequest(`/products/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateProductStatus(id, status) {
  return apiRequest(`/products/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function uploadProductImage(id, file) {
  return uploadFile(`/products/${id}/image`, file)
}

export function getPosProducts() {
  return apiRequest('/pos/products')
}

export function searchProducts(query) {
  return apiRequest(`/pos/products/search?query=${encodeURIComponent(query)}`)
}

export function lookupBarcode(barcode) {
  return apiRequest(`/pos/products/barcode/${encodeURIComponent(barcode)}`)
}
