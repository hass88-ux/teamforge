import { useEffect, useState } from 'react'
import { accountRequest } from './accountApi'

type Report = { id: string; reporter_id: string; reported_id: string; reason: string; details: string; created_at: string; review_status: string; review_note: string; revision: number }
export default function ModerationPanel({ onBack }: { onBack: () => void }) {
 const [rows, setRows] = useState<Report[]>([]), [offset, setOffset] = useState(0), [more, setMore] = useState(false)
 const [status, setStatus] = useState('OPEN'), [reload, setReload] = useState(0), [note, setNote] = useState('')
 const [selected, setSelected] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [loading, setLoading] = useState(true)
 useEffect(() => {
  let active = true
  async function load() {
   setLoading(true); setError('')
   try {
    const response = await accountRequest(`/api/moderation/reports?status=${status}&offset=${offset}`)
    if (!response.ok) throw new Error(response.status === 403 ? 'Report review is restricted to authorized moderators.' : 'Cannot load reports. Try again.')
    const data = await response.json()
    if (active) { setRows(data.reports); setMore(data.hasMore) }
   } catch (failure) { if (active) setError(failure instanceof Error ? failure.message : 'Cannot reach the service.') }
   finally { if (active) setLoading(false) }
  }
  void load(); return () => { active = false }
 }, [status, offset, reload])
 async function review(row: Report, outcome: string) {
  if (busy || !note.trim()) return
  setBusy(true); setError('')
  try {
   const response = await accountRequest(`/api/moderation/reports/${row.id}/review`, 'POST', { revision: row.revision, outcome, note })
   if (!response.ok) throw new Error(response.status === 409 ? 'This report was already reviewed. Refresh to see the latest state.' : 'Cannot save this review.')
   setSelected(''); setNote(''); setReload(value => value + 1)
  } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach the service.') }
  finally { setBusy(false) }
 }
 return <section className="onboarding"><button className="text-button" onClick={onBack}>Back to account</button><h1>Report review</h1><p>Keep report details private. Closing a match ends messaging for both members; it does not remove shared project access or ban an account.</p><label className="input-label">Queue <select disabled={busy} value={status} onChange={event => { setStatus(event.target.value); setOffset(0); setSelected(''); setNote('') }}><option value="OPEN">Open</option><option value="DISMISSED">Dismissed</option><option value="MATCH_CLOSED">Match closed</option></select></label><button className="secondary" disabled={busy || loading} onClick={() => setReload(value => value + 1)}>Refresh</button>{error && <p role="alert" className="error">{error}</p>}{loading ? <p role="status">Loading reports…</p> : rows.length === 0 ? <p>No reports in this queue.</p> : rows.map(row => <article className="preview" key={row.id}><h2>{row.reason}</h2><p>{row.details || 'No additional details.'}</p><p className="note">Reported {new Date(row.created_at).toLocaleString()} · Reporter {row.reporter_id} · Reported account {row.reported_id}</p>{status === 'OPEN' ? selected === row.id ? <><label className="input-label" htmlFor="review-note">Private review note</label><textarea id="review-note" maxLength={1000} value={note} onChange={event => setNote(event.target.value)} /><div className="actions"><button disabled={busy || !note.trim()} onClick={() => void review(row, 'MATCH_CLOSED')}>Close match & resolve</button><button className="secondary" disabled={busy || !note.trim()} onClick={() => void review(row, 'DISMISSED')}>Dismiss report</button><button className="text-button" disabled={busy} onClick={() => setSelected('')}>Cancel</button></div></> : <button className="secondary" disabled={busy} onClick={() => { setSelected(row.id); setNote('') }}>Review report</button> : <p>Review: {row.review_note}</p>}</article>)}<div className="actions"><button className="secondary" disabled={offset === 0 || busy || loading} onClick={() => setOffset(value => Math.max(0, value - 25))}>Previous</button><button className="secondary" disabled={!more || busy || loading} onClick={() => setOffset(value => value + 25)}>Next</button></div></section>
}
