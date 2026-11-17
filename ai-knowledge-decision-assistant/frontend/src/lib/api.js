const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export function getToken() { return localStorage.getItem('accessToken') }
export function setSession(data) { localStorage.setItem('accessToken', data.accessToken); localStorage.setItem('profile', JSON.stringify(data)) }
export function clearSession() { localStorage.removeItem('accessToken'); localStorage.removeItem('profile') }

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (response.status === 204) return null
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.error || data.message || `Request failed (${response.status})`)
  return data
}

export const apiBase = API_BASE
