import { useState } from 'react'
import { accountRequest } from './accountApi'

export default function RecommendationFeedback({ token }: { token: string }) {
 const [rating, setRating] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
 async function send(value: string) {
  if (busy) return
  setBusy(true); setError('')
  try {
   const response = await accountRequest('/api/discovery/feedback', 'POST', { token, rating: value })
   if (!response.ok) throw new Error(response.status === 409 ? 'This recommendation expired. Refresh discovery to give feedback.' : 'Could not save feedback. Please try again.')
   setRating(value)
  } catch (e) { setError(e instanceof Error ? e.message : 'Could not save feedback.') }
  finally { setBusy(false) }
 }
 return <section className="preview" aria-label="Optional recommendation feedback"><h3>Is this recommendation relevant?</h3><p className="note">Optional: save your rating with this recommendation’s scoring signals for future matching evaluation. This does not like, pass, message, or change live ranking. Download or clear your ratings in Account settings.</p><div className="actions"><button className="secondary" aria-pressed={rating === 'RELEVANT'} disabled={busy} onClick={() => void send('RELEVANT')}>Relevant</button><button className="secondary" aria-pressed={rating === 'NOT_RELEVANT'} disabled={busy} onClick={() => void send('NOT_RELEVANT')}>Not relevant</button></div>{rating && <p role="status">Feedback saved. You can change your rating.</p>}{error && <p className="error" role="alert">{error}</p>}</section>
}
