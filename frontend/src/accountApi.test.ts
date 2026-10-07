import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { accountRequest, resetSecureSession } from './accountApi'
const fetchMock = vi.fn()
beforeEach(() => {
  resetSecureSession()
  vi.stubGlobal('fetch', fetchMock)
  vi.stubGlobal('window', { setTimeout: globalThis.setTimeout, clearTimeout: globalThis.clearTimeout })
})
afterEach(() => { vi.unstubAllGlobals(); fetchMock.mockReset(); vi.useRealTimers() })
const token = () => new Response(JSON.stringify({ headerName: 'X-CSRF-TOKEN', token: 'secure-token' }))
test('concurrent writes share token initialization without replaying writes', async () => {
  fetchMock.mockImplementation(async (path: string) => path === '/api/auth/csrf' ? token() : new Response(null, { status: 204 }))
  await Promise.all([accountRequest('/one', 'POST', {}), accountRequest('/two', 'PUT', {})])
  expect(fetchMock.mock.calls.filter(([path]) => path === '/api/auth/csrf')).toHaveLength(1)
  expect(fetchMock.mock.calls.filter(([path]) => path !== '/api/auth/csrf')).toHaveLength(2)
  expect(fetchMock.mock.calls.find(([path]) => path === '/one')?.[1].headers['X-CSRF-TOKEN']).toBe('secure-token')
})
test('login rotates cached token and rejected writes require an explicit retry', async () => {
  fetchMock.mockImplementation(async (path: string) => path === '/api/auth/csrf' ? token() : new Response(null, { status: path === '/rejected' ? 403 : 204 }))
  await accountRequest('/first', 'POST')
  await accountRequest('/api/auth/login', 'POST', {})
  await accountRequest('/rejected', 'POST')
  expect(fetchMock.mock.calls.filter(([path]) => path === '/rejected')).toHaveLength(1)
  await accountRequest('/retry', 'POST')
  expect(fetchMock.mock.calls.filter(([path]) => path === '/api/auth/csrf')).toHaveLength(3)
})
test('bounded waiting aborts once and does not retry an uncertain write', async () => {
  vi.useFakeTimers()
  vi.stubGlobal('window', { setTimeout: globalThis.setTimeout, clearTimeout: globalThis.clearTimeout })
  fetchMock.mockImplementation((path: string, options: RequestInit) => path === '/api/auth/csrf' ? Promise.resolve(token()) : new Promise((_resolve, reject) => options.signal?.addEventListener('abort', () => reject(new Error('aborted')))))
  const request = accountRequest('/write', 'POST', {})
  const assertion = expect(request).rejects.toThrow('Check your latest activity')
  await vi.advanceTimersByTimeAsync(90001)
  await assertion
  expect(fetchMock.mock.calls.filter(([path]) => path === '/write')).toHaveLength(1)
})
