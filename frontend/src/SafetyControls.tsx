import { useRef, useState } from 'react'
import { accountRequest } from './accountApi'
export default function SafetyControls({ matchId, targetId, onBlocked }: { matchId: string; targetId: string; onBlocked: () => void }) {
 const [confirm, setConfirm] = useState(false), [showReport, setShowReport] = useState(false), [busy, setBusy] = useState(false)
 const [reason, setReason] = useState('Harassment'), [details, setDetails] = useState(''), [error, setError] = useState(''), [notice, setNotice] = useState('')
 const retry = useRef<{ key: string; id: string } | null>(null)
 async function block() {
  if (busy) return
  setBusy(true); setError('')
  try {
   const r = await accountRequest('/api/safety/blocks', 'POST', { targetId })
   if (!r.ok) throw new Error('Cannot block this person. Please try again.')
   onBlocked()
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot save block.') }
  finally { setBusy(false) }
 }
 async function report() {
  if (busy) return
  setBusy(true); setError(''); setNotice('')
  const draft = { matchId, reason, details: details.trim() }, key = JSON.stringify(draft)
  if (retry.current?.key !== key) retry.current = { key, id: crypto.randomUUID() }
  try {
   const r = await accountRequest('/api/safety/reports', 'POST', { ...draft, clientId: retry.current.id })
   if (!r.ok) throw new Error(r.status === 429 ? 'Report limit reached. Try again tomorrow. Blocking is still available.' : 'Cannot record this report. Please try again.')
   const result = await r.json()
   setNotice(`Report recorded: ${result.id}. This local preview has no staffed review or notification system yet.`); setShowReport(false)
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot save report.') }
  finally { setBusy(false) }
 }
 return <section className="liked-list"><h2>Manage contact</h2>{error && <p className="error" role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}<div className="actions"><button className="secondary" disabled={busy} onClick={() => setConfirm(true)}>Block this person</button><button className="text-button" disabled={busy} onClick={() => setShowReport(v => !v)}>Report this conversation</button></div>{confirm && <div role="alert"><p>Block this person? Both profiles disappear from each other’s discovery and this conversation closes. Shared project memberships remain; leave or remove a teammate in Projects & teams. Unblocking is not available in this preview.</p><button disabled={busy} onClick={() => void block()}>Confirm block</button><button className="secondary" disabled={busy} onClick={() => setConfirm(false)}>Keep contact</button></div>}{showReport && <div><label className="input-label" htmlFor="report-reason">Report reason</label><select id="report-reason" value={reason} onChange={e => setReason(e.target.value)}>{['Harassment', 'Spam', 'Impersonation', 'Other'].map(r => <option key={r}>{r}</option>)}</select><label className="input-label" htmlFor="report-details">Report details — optional</label><textarea id="report-details" maxLength={1000} value={details} onChange={e => setDetails(e.target.value)}/><p className="note">Reports are saved locally. Reporting does not block contact or notify a moderator in this preview.</p><button disabled={busy} onClick={() => void report()}>Record report</button></div>}</section>
}
