export async function accountRequest(path: string, method = 'GET', body?: unknown): Promise<Response> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (!['GET', 'HEAD'].includes(method)) {
    const tokenResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    if (!tokenResponse.ok) throw new Error('Cannot initialize your secure session. Please try again.')
    const csrf = await tokenResponse.json()
    headers[csrf.headerName] = csrf.token
  }
  return fetch(path, { method, credentials: 'same-origin', headers, body: body === undefined ? undefined : JSON.stringify(body) })
}
