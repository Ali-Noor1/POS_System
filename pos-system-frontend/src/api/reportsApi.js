import { apiRequest } from './client'

function reportQuery(startDate, endDate) {
  return `startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`
}

export function getSalesReport(startDate, endDate) {
  return apiRequest(`/reports/sales?${reportQuery(startDate, endDate)}`)
}

export function getProductSalesReport(startDate, endDate) {
  return apiRequest(`/reports/product-sales?${reportQuery(startDate, endDate)}`)
}

export function getInventoryMovementReport(startDate, endDate) {
  return apiRequest(`/reports/inventory-movements?${reportQuery(startDate, endDate)}`)
}
