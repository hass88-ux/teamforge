import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { accountRequest } from './accountApi'
import './DemoConversation.css'
import CoffeeInvitations from './CoffeeInvitations'
import SafetyControls from './SafetyControls'
import PublicProfile from './PublicProfile'

export type RealMatch = { id: string; partnerId: string; lastMessage?: string; unreadCount?: number; displayName: string; entityType: string; matchingIntent: string; compatibility: number }
type Message = { sequence: number; clientId: string; fromYou: boolean; text: string; sentAt: string }
export default function RealConversation({ match, onBack, onUnmatched, backLabel = 'Back to discovery' }: { match: RealMatch; backLabel?: string; onBack: () => void; onUnmatched: () => void }) {
  const [viewProfile, setViewProfile] = useState(false)
  const polling = useRef(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [hasMore, setHasMore] = useState(false)
  const [confirmEnd, setConfirmEnd] = useState(false)
  const pending = useRef<{ clientId: string; text: string } | null>(null)
  const cursor = useRef(0)
  const acknowledged = useRef(0)
  const idlePolls = useRef(0)
  const acknowledge = useCallback(async (sequence: number) => {
    if (document.visibilityState !== 'visible' || sequence <= acknowledged.current) return
    try { const response = await accountRequest(`/api/matches/${match.id}/read`, 'POST', { sequence }); if (response.ok) acknowledged.current = Math.max(acknowledged.current, sequence) } catch { /* Retry acknowledgement on the next successful poll. */ }
  }, [match.id])
  const load = useCallback(async () => {
    if (polling.current) return; polling.current = true
    setLoading(true); setError('')
    try {
      const response = await accountRequest(`/api/matches/${match.id}/messages?after=${cursor.current}`)
      if (!response.ok) throw new Error(response.status === 404 ? 'This conversation is no longer available.' : response.status === 401 ? 'Your session expired. Please log in again.' : 'Cannot load messages. Please try again.')
      const result = await response.json()
      setMessages(current => [...current, ...result.messages.filter((message: Message) => !current.some(item => item.sequence === message.sequence))].sort((a, b) => a.sequence - b.sequence))
      idlePolls.current = result.nextAfter > cursor.current ? 0 : idlePolls.current + 1; cursor.current = result.nextAfter; setHasMore(result.hasMore); void acknowledge(result.nextAfter)
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach messages.') }
    finally { setLoading(false); polling.current = false }
  }, [match.id, acknowledge])
  useEffect(() => {
    let active = true
    accountRequest(`/api/matches/${match.id}/messages?after=0`).then(async response => {
      if (!response.ok) throw new Error(response.status === 404 ? 'This conversation is no longer available.' : 'Cannot load messages. Please try again.')
      return response.json()
    }).then(result => { if (active) { setMessages(result.messages); idlePolls.current = result.nextAfter > cursor.current ? 0 : idlePolls.current + 1; cursor.current = result.nextAfter; setHasMore(result.hasMore); void acknowledge(result.nextAfter) } }).catch(failure => { if (active) setError(failure instanceof Error ? failure.message : 'Cannot reach messages.') }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [match.id, acknowledge])
  useEffect(() => {
    const timer = window.setInterval(() => { if (!busy && !loading && !viewProfile && document.visibilityState === 'visible') void load() }, idlePolls.current >= 3 ? 30000 : 15000)
    function focus() { if (!busy && !viewProfile && document.visibilityState === 'visible') { idlePolls.current = 0; void load() } }
    document.addEventListener('visibilitychange', focus); window.addEventListener('focus', focus)
    return () => { clearInterval(timer); document.removeEventListener('visibilitychange', focus); window.removeEventListener('focus', focus) }
  }, [busy, loading, viewProfile, load])
  async function send(event: FormEvent) {
    event.preventDefault()
    if (busy || !text.trim()) return
    setBusy(true); setError('')
    // Retain the request ID after a network failure so Retry cannot send the same message twice.
    if (!pending.current || pending.current.text !== text.trim()) pending.current = { clientId: crypto.randomUUID(), text: text.trim() }
    try {
      const response = await accountRequest(`/api/matches/${match.id}/messages`, 'POST', pending.current)
      if (!response.ok) throw new Error(response.status === 404 ? 'This conversation is no longer available.' : response.status === 429 ? 'You’re sending quickly. Wait a minute and try again.' : 'Message not confirmed. Try again; retries will not duplicate it.')
      const saved: Message = await response.json()
      setMessages(current => current.some(item => item.sequence === saved.sequence) ? current : [...current, saved].sort((a, b) => a.sequence - b.sequence))
      pending.current = null; setText(''); void load()
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Message not confirmed. Please retry.') }
    finally { setBusy(false) }
  }
  async function unmatch() {
    setBusy(true); setError('')
    try {
      const response = await accountRequest(`/api/matches/${match.id}`, 'DELETE')
      if (!response.ok && response.status !== 404) throw new Error('Cannot end this match. Please try again.')
      onUnmatched()
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach your match.') }
    finally { setBusy(false) }
  }
  if (viewProfile) return <PublicProfile id={match.partnerId} onBack={() => setViewProfile(false)}/>
  return <section className="conversation"><div className="onboarding-top"><span className="demo-label">YOUR CONVERSATION</span><button className="text-button" onClick={onBack}>{backLabel}</button></div><h1>Say hello to <em>{match.displayName}.</em></h1><p>{match.entityType === 'ORGANIZATION' ? 'Organization' : 'Individual'} · {match.matchingIntent.toLowerCase()} · {match.compatibility}% compatibility when matched</p><button className="text-button" onClick={() => setViewProfile(true)}>View collaborator profile</button><p className="note">Messages are saved to your conversation. Only the two matched members can access them. New replies refresh automatically every 15–30 seconds while this screen is visible; you can also refresh manually.</p>
    {error && <p className="error" role="alert">{error}</p>}
    <div className="message-list" role="log" aria-label="Conversation messages" aria-live="polite">{!loading && !messages.length && <p>Introduce yourself and share something you’d like to build.</p>}{messages.map(message => <article key={message.sequence} className={`message ${message.fromYou ? 'you' : 'received'}`}><strong>{message.fromYou ? 'You' : match.displayName}</strong><p>{message.text}</p><time dateTime={message.sentAt}>{new Date(message.sentAt).toLocaleString()}</time></article>)}</div>
    <button className="secondary" disabled={loading || busy} onClick={() => void load()}>{loading ? 'Loading…' : hasMore ? 'Load more messages' : 'Refresh messages'}</button>
    <form onSubmit={send}><label className="input-label" htmlFor="real-message">Your message</label><textarea id="real-message" maxLength={1000} value={text} onChange={event => setText(event.target.value)} placeholder="What would you like to build together?"/><div className="onboarding-controls"><span className="note">{text.length} / 1000</span><button disabled={busy || !text.trim()}>{busy ? 'Sending…' : 'Send message'}</button></div></form>
    <CoffeeInvitations matchId={match.id} partner={match.displayName}/><SafetyControls matchId={match.id} targetId={match.partnerId} onBlocked={onUnmatched}/><section className="liked-list"><button className="text-button" disabled={busy} onClick={() => setConfirmEnd(true)}>Unmatch</button>{confirmEnd && <div role="alert"><p>End this match? Both people will lose access to this conversation and won’t be able to match or message each other again in this preview.</p><div className="actions"><button disabled={busy} onClick={() => void unmatch()}>End match</button><button className="secondary" disabled={busy} onClick={() => setConfirmEnd(false)}>Keep match</button></div></div>}</section>
  </section>
}
