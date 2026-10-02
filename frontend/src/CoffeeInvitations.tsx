import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { accountRequest } from './accountApi'

type Proposal = { id: string; fromYou: boolean; kind: string; startsAt: string; timezone: string; note: string; status: string }
export default function CoffeeInvitations({ matchId, partner }: { matchId: string; partner: string }) {
  const [clock, setClock] = useState(() => Date.now())
  const [proposals, setProposals] = useState<Proposal[]>([])
  const [kind, setKind] = useState('Virtual coffee')
  const [date, setDate] = useState('')
  const [time, setTime] = useState('18:00')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const pending = useRef<{ key: string; clientId: string } | null>(null)
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  useEffect(() => {
    let active = true
    accountRequest(`/api/matches/${matchId}/proposals`).then(async response => {
      if (!response.ok) throw new Error(response.status === 404 ? 'This match is no longer available.' : 'Cannot load invitations. Please refresh.')
      return response.json()
    }).then(result => { if (active) { setProposals(result); setClock(Date.now()); setError('') } }).catch(failure => { if (active) setError(failure instanceof Error ? failure.message : 'Cannot reach invitations.') }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [matchId, attempt])
  async function propose(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    const startsAt = new Date(`${date}T${time}`)
    const [year, month, day] = date.split('-').map(Number)
    const [hour, minute] = time.split(':').map(Number)
    if (!Number.isFinite(startsAt.getTime()) || startsAt.getFullYear() !== year || startsAt.getMonth() + 1 !== month || startsAt.getDate() !== day || startsAt.getHours() !== hour || startsAt.getMinutes() !== minute || startsAt.getTime() <= Date.now()) { setError('Choose a valid future date and time in your timezone.'); return }
    const draft = { kind, startsAt: startsAt.toISOString(), timezone, note: note.trim() }
    const key = JSON.stringify(draft)
    if (!pending.current || pending.current.key !== key) pending.current = { key, clientId: crypto.randomUUID() }
    setBusy(true); setError('')
    try {
      const response = await accountRequest(`/api/matches/${matchId}/proposals`, 'POST', { ...draft, clientId: pending.current.clientId })
      if (!response.ok) throw new Error(response.status === 400 ? 'Choose a valid time within the next 180 days.' : response.status === 429 ? 'This conversation already has ten pending invitations. Resolve or cancel one first.' : 'Invitation not confirmed. Retry with the same details to avoid duplicates.')
      const saved: Proposal = await response.json()
      setAttempt(value => value + 1); setProposals(current => [saved, ...current.filter(item => item.id !== saved.id)]); pending.current = null; setDate(''); setNote('')
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot save invitation.') }
    finally { setBusy(false) }
  }
  async function respond(id: string, status: string) {
    if (busy) return
    setBusy(true); setError('')
    try {
      const response = await accountRequest(`/api/matches/${matchId}/proposals/${id}/response`, 'POST', { status })
      if (!response.ok) throw new Error(response.status === 409 ? 'This invitation changed or its time has passed. Refresh to see the latest status.' : 'Cannot update invitation. Please refresh and try again.')
      const saved: Proposal = await response.json(); setAttempt(value => value + 1); setProposals(current => current.map(item => item.id === saved.id ? saved : item))
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot update invitation.') }
    finally { setBusy(false) }
  }
  return <section className="coffee-panel liked-list"><h2>Plan a conversation</h2><p className="note">Invite {partner} to a 30-minute conversation. Times display in {timezone}. Invitations stay in this app; no calendar event, meeting link, or email is created.</p>
    <form onSubmit={propose}><label className="input-label" htmlFor="real-coffee-kind">Conversation type</label><select id="real-coffee-kind" value={kind} onChange={event => setKind(event.target.value)}><option>Virtual coffee</option><option>Intro call</option><option>Project discussion</option></select><label className="input-label" htmlFor="real-coffee-date">Date (YYYY-MM-DD)</label><input id="real-coffee-date" required type="text" pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}" maxLength={10} placeholder="2026-10-05" value={date} onChange={event => setDate(event.target.value)}/><label className="input-label" htmlFor="real-coffee-time">Time ({timezone})</label><select id="real-coffee-time" value={time} onChange={event => setTime(event.target.value)}>{Array.from({ length: 48 }, (_, index) => { const value = `${String(Math.floor(index / 2)).padStart(2, '0')}:${index % 2 ? '30' : '00'}`; return <option key={value}>{value}</option> })}</select><label className="input-label" htmlFor="real-coffee-note">Optional invitation note</label><textarea id="real-coffee-note" maxLength={500} value={note} onChange={event => setNote(event.target.value)}/><button disabled={busy || loading || !date}>{busy ? 'Saving…' : 'Send invitation'}</button></form>
    {error && <p role="alert" className="error">{error}</p>}<button className="secondary" disabled={busy || loading} onClick={() => { setLoading(true); setAttempt(value => value + 1) }}>{loading ? 'Loading invitations…' : 'Refresh invitations'}</button>
    <div aria-live="polite">{proposals.map(proposal => { const upcoming = Date.parse(proposal.startsAt) > clock; return <article className="proposal" key={proposal.id}><h3>{proposal.kind}</h3><p>{proposal.fromYou ? 'You invited' : 'Invitation from'} {partner}</p><p><time dateTime={proposal.startsAt}>{new Date(proposal.startsAt).toLocaleString()}</time> · {timezone} · 30 minutes</p>{proposal.timezone !== timezone && <p>Proposed in {proposal.timezone}</p>}{proposal.note && <p>{proposal.note}</p>}<span className="demo-label">{proposal.status.toLowerCase()}{!upcoming && ' · time passed'}</span>{upcoming && <div className="actions">{proposal.status === 'PROPOSED' && !proposal.fromYou && <><button disabled={busy} onClick={() => void respond(proposal.id, 'ACCEPTED')}>Accept invitation</button><button className="secondary" disabled={busy} onClick={() => void respond(proposal.id, 'DECLINED')}>Decline invitation</button></>}{['PROPOSED', 'ACCEPTED'].includes(proposal.status) && <button className="secondary" disabled={busy} onClick={() => void respond(proposal.id, 'CANCELLED')}>Cancel invitation</button>}</div>}</article> })}</div>
  </section>
}
