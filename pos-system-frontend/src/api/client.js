const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const API_ORIGIN = new URL(API_BASE_URL, globalThis.location?.origin || 'http://localhost:8080').origin

function getStoredSession() {
  try {
    const rawSession = localStorage.getItem('posSession')
    return rawSession ? JSON.parse(rawSession) : null
  } catch {
    return null
  }
}

export function saveSession(session) {
  localStorage.setItem('posSession', JSON.stringify(session))
}

export function loadSession() {
  return getStoredSession()
}

export function clearSession() {
  localStorage.removeItem('posSession')
}

export async function apiRequest(path, options = {}) {
  const session = getStoredSession()
  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    ...options.headers,
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = data?.message || data?.error || data || 'Request failed'
    throw new Error(message)
  }

  return data
}

export async function uploadFile(path, file) {
  const session = getStoredSession()
  const formData = new FormData()
  formData.append('image', file)

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
    },
    body: formData,
  })

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = data?.message || data?.error || data || 'File upload failed'
    throw new Error(message)
  }

  return data
}

export function assetUrl(path) {
  if (!path) {
    return ''
  }

  if (/^https?:\/\//i.test(path)) {
    return path
  }

  return `${API_ORIGIN}${path.startsWith('/') ? path : `/${path}`}`
}
