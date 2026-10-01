import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { accountRequest } from './accountApi'
import Onboarding from './Onboarding'
import type { ProfileDraft } from './profile'

type AccountView = { id: string; email: string; accountType: 'REAL' }
export default function Account({ initialMode, onExit }: { initialMode: 'login' | 'signup'; onExit: () => void }) {
  const [mode, setMode] = useState(initialMode)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [account, setAccount] = useState<AccountView | null>(null)
  const [profile, setProfile] = useState<ProfileDraft | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    async function restore() {
      try {
        const response = await accountRequest('/api/auth/me')
        if (response.status === 401) return
        if (!response.ok) throw new Error('Account service is unavailable. Try again shortly.')
        const user = await response.json()
        const saved = await accountRequest('/api/profiles/me')
        if (!saved.ok && saved.status !== 404) throw new Error('Cannot load your saved profile. Please try again.')
        const draft = saved.ok ? (await saved.json()).profile : undefined
        if (active) { setProfile(draft); setAccount(user) }
      } catch (failure) { if (active) setError(failure instanceof Error ? failure.message : 'Cannot reach the account service.') }
      finally { if (active) setLoading(false) }
    }
    void restore(); return () => { active = false }
  }, [])
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    if (new TextEncoder().encode(password).length > 72) { setError('Use a password of at most 72 UTF-8 bytes.'); return }
    setBusy(true); setError('')
    try {
      const response = await accountRequest(`/api/auth/${mode}`, 'POST', { email: email.trim(), password })
      if (!response.ok) throw new Error(response.status === 401 ? 'Email or password is incorrect.' : response.status === 409 ? 'Unable to register with these details. Try logging in or use a different email.' : response.status === 429 ? 'Too many attempts. Please try again in five minutes.' : 'Unable to complete authentication. Check your details and try again.')
      const user = await response.json()
      const saved = await accountRequest('/api/profiles/me')
      if (!saved.ok && saved.status !== 404) throw new Error('Signed in, but unable to load the profile. Please reopen your account.')
      setProfile(saved.ok ? (await saved.json()).profile : undefined); setAccount(user); setPassword('')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach the account service.') }
    finally { setBusy(false) }
  }
  async function logout() {
    setBusy(true); setError('')
    try {
      const response = await accountRequest('/api/auth/logout', 'POST')
      if (!response.ok) throw new Error('Unable to log out. Please try again.')
      setAccount(null); setProfile(undefined); onExit()
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach the account service.') }
    finally { setBusy(false) }
  }
  if (loading) return <section className="onboarding"><p role="status">Checking your session…</p></section>
  if (account) return <><div className="account-strip"><span>Signed in as {account.email} · Real account</span><button className="secondary" onClick={() => void logout()} disabled={busy}>Log out</button></div>{error && <p className="error" role="alert">{error}</p>}<Onboarding key={account.id} mode="REAL" initialProfile={profile} onExit={onExit}/></>
  return <section className="auth-form"><span className="demo-label">REAL ACCOUNT · YOUR PROFILE IS SAVED</span><h1>{mode === 'signup' ? 'Start building.' : 'Welcome back.'}</h1><p className="onboarding-subtitle">{mode === 'signup' ? 'Create your account and build your collaboration profile.' : 'Log in to edit your saved collaboration profile.'}</p><p className="note">Real-user discovery and collaboration are still in development. Try demo to explore the complete fictional journey.</p><form onSubmit={submit}><label className="input-label" htmlFor="account-email">Email</label><input id="account-email" type="email" autoComplete="email" maxLength={254} required value={email} onChange={event => setEmail(event.target.value)}/><label className="input-label" htmlFor="account-password">Password</label><input id="account-password" type="password" autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} minLength={12} maxLength={72} required value={password} onChange={event => setPassword(event.target.value)}/><p className="note">At least 12 characters. Passwords are hashed; they are never part of your public profile. No email verification or password reset is available yet.</p>{error && <p className="error" role="alert">{error}</p>}<div className="actions"><button disabled={busy}>{busy ? 'Please wait…' : mode === 'signup' ? 'Sign up' : 'Log in'}</button><button type="button" className="secondary" disabled={busy} onClick={() => { setMode(mode === 'signup' ? 'login' : 'signup'); setError('') }}>{mode === 'signup' ? 'Already have an account?' : 'Create an account'}</button></div></form><button className="text-button" onClick={onExit}>Back to home</button></section>
}
