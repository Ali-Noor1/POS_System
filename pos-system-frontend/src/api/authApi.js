import { apiRequest, saveSession } from './client'

export async function login(username, password) {
  const response = await apiRequest('/auth/login', {
    method: 'POST',
    body: { username, password },
  })

  const session = {
    id: response.id,
    fullName: response.fullName,
    username: response.username,
    role: response.role,
    token: response.token,
    tokenType: response.tokenType,
  }

  saveSession(session)
  return session
}
