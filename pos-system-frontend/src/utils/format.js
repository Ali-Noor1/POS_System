export function money(value) {
  return Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export function normalizeRole(role) {
  return String(role || '').replace('ROLE_', '').toUpperCase()
}

export function todayDate() {
  return new Date().toISOString().slice(0, 10)
}

export function defaultStartDate() {
  const date = new Date()
  date.setDate(date.getDate() - 30)
  return date.toISOString().slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  return new Date(value).toLocaleString()
}

export function formatEnum(value) {
  return String(value || '-').replaceAll('_', ' ')
}
