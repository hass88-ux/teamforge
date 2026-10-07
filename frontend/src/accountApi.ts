type Csrf = { headerName: string; token: string }
let csrf: Promise<Csrf> | null = null
export function resetSecureSession() { csrf = null }
async function timedFetch(path: string, options: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 90000)
  try { return await fetch(path, { ...options, signal: controller.signal }) }
  catch (error) {
    if (controller.signal.aborted) throw new Error('The server is taking longer than expected to wake up. Check your latest activity before retrying a change.')
    throw error
  } finally { window.clearTimeout(timer) }
}
async function secureToken(): Promise<Csrf> {
  if (!csrf) {
    const pending = timedFetch('/api/auth/csrf', { credentials: 'same-origin' }).then(async response => {
      if (!response.ok) throw new Error('Cannot initialize your secure session. Please try again.')
      return response.json() as Promise<Csrf>
    })
    csrf = pending
    void pending.catch(() => { if (csrf === pending) csrf = null })
  }
  return csrf
}
export async function accountRequest(path: string, method = 'GET', body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (!['GET', 'HEAD'].includes(method)) {
    const token = await secureToken()
    headers[token.headerName] = token.token
  }
  const response = await timedFetch(path, { method, credentials: 'same-origin', headers, body: body === undefined ? undefined : JSON.stringify(body) })
  if (response.status === 401 || response.status === 403 || (method !== 'GET' && /^\/api\/auth\/(login|signup|logout|recover)$/.test(path)) || (method === 'POST' && path === '/api/account/delete')) resetSecureSession()
  // Never automatically replay a write: a timed-out request may already have committed.
  return response
}
