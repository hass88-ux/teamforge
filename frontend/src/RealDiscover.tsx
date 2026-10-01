import { useEffect, useRef, useState } from 'react'
import { accountRequest } from './accountApi'
import RealConversation from './RealConversation'
import type { RealMatch } from './RealConversation'
import type { ProfileDraft } from './profile'

type PublicProfile = Omit<ProfileDraft, 'availability' | 'timezone'> & { id: string; accountType: 'REAL' }
type Result = { candidate: PublicProfile; compatibility: number; evidence: string[] }
export default function RealDiscover({ onBack }: { onBack: () => void }) {
  const [matches, setMatches] = useState<RealMatch[]>([])
  const [activeMatch, setActiveMatch] = useState<RealMatch | null>(null)
  const [matchError, setMatchError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const gesture = useRef<{ x: number; y: number } | null>(null)
  const [results, setResults] = useState<Result[]>([])
  const [visible, setVisible] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [index, setIndex] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [limited, setLimited] = useState(false)
  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true); setError('')
      try {
        const [profile, discovery] = await Promise.all([accountRequest('/api/profiles/me'), accountRequest('/api/discovery/recommendations')])
        if (profile.status === 401 || discovery.status === 401) throw new Error('Your session expired. Please log in again.')
        if (!profile.ok || !discovery.ok) throw new Error('Cannot load discovery right now. Your profile is still saved; try again.')
        const saved = await profile.json(); const ranked = await discovery.json()
        if (ranked.accountType !== 'REAL' || !Array.isArray(ranked.recommendations) || ranked.recommendations.some((item: Result) => item.candidate.accountType !== 'REAL')) throw new Error('Unexpected discovery response. Please try again.')
        if (active) { setVisible(saved.discoverable); setResults(ranked.recommendations); setLimited(ranked.poolLimited); setIndex(0) }
      } catch (failure) { if (active) setError(failure instanceof Error ? failure.message : 'Cannot reach discovery.') }
      finally { if (active) setLoading(false) }
    }
    void load(); return () => { active = false }
  }, [attempt])
  useEffect(() => {
    let active = true
    accountRequest('/api/matches').then(async response => {
      if (!response.ok) throw new Error('Cannot load your matches. Please try again.')
      return response.json()
    }).then(result => { if (active) { setMatches(result); setMatchError('') } }).catch(failure => { if (active) setMatchError(failure instanceof Error ? failure.message : 'Cannot load matches.') })
    return () => { active = false }
  }, [attempt])
  async function decide(decision: 'LIKE' | 'PASS') {
    if (busy || !results[index]) return
    setBusy(true); setActionError(''); setNotice('')
    try {
      const response = await accountRequest('/api/discovery/decisions', 'POST', { candidateId: results[index].candidate.id, decision })
      if (!response.ok) throw new Error(response.status === 409 ? 'Your profile or their availability changed. Enable visibility to like, or refresh discovery and try again.' : response.status === 404 ? 'This profile is no longer available. Refresh discovery.' : 'Cannot save this choice. Please try again.')
      const result = await response.json()
      setIndex(value => value + 1)
      if (result.matched) { setNotice('You matched! Open your conversation below.'); setAttempt(value => value + 1) }
      else setNotice(decision === 'LIKE' ? 'Like saved. A match appears when they like you back.' : 'Pass saved.')
    } catch (failure) { setActionError(failure instanceof Error ? failure.message : 'Cannot save this choice.') }
    finally { setBusy(false) }
  }
  async function visibility() {
    if (busy) return
    setBusy(true); setError('')
    try {
      const response = await accountRequest('/api/profiles/me/visibility', 'PUT', { discoverable: !visible })
      if (!response.ok) throw new Error('Cannot update visibility. Please try again.')
      const saved = await response.json(); setVisible(saved.discoverable)
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Cannot reach your profile.') }
    finally { setBusy(false) }
  }
  const current = results[index]
  if (activeMatch) return <RealConversation key={activeMatch.id} match={activeMatch} onBack={() => { setActiveMatch(null); setAttempt(value => value + 1) }} onUnmatched={() => { setActiveMatch(null); setAttempt(value => value + 1); setNotice('Match ended.') }} />
  return <section className="discover"><div className="onboarding-top"><span className="demo-label">YOUR NETWORK</span><button className="text-button" onClick={onBack}>Your profile</button></div><h1>Find people<br/><em>ready to build.</em></h1>
    <section className="preview"><h2>Profile visibility</h2><p>{visible === null ? 'Loading your profile visibility…' : `Your profile is ${visible ? 'visible to signed-in members' : 'private'}.`} When enabled, discovery shares your name, description, type, intent, skills, interests, role preferences, goal, working style, and weekly commitment. Your email and selected schedule stay private.</p><button className="secondary" disabled={loading || busy || visible === null} onClick={() => void visibility()}>{busy ? 'Saving…' : visible ? 'Hide my profile from discovery' : 'Show my profile in discovery'}</button><p className="note">You can browse while private. Enable visibility to like people. Hiding your profile stops new discovery; existing matches stay available until you unmatch.</p></section>
    {error && <div className="error" role="alert"><p>{error}</p><button disabled={loading || busy} onClick={() => setAttempt(value => value + 1)}>Try again</button></div>}
    {notice && <p role="status" className="demo-disclosure">{notice}</p>}
    {actionError && <p role="alert" className="error">{actionError}</p>}
    {matchError && <div role="alert" className="error"><p>{matchError}</p><button onClick={() => setAttempt(value => value + 1)}>Retry matches</button></div>}
    {matches.length > 0 && <section className="liked-list"><h2>Your matches</h2><div className="match-list">{matches.map(match => <button className="secondary" key={match.id} onClick={() => setActiveMatch(match)}><strong>{match.displayName}</strong><span>{match.compatibility}% · Open conversation →</span></button>)}</div></section>}
    {loading && <p role="status">Finding compatible profiles…</p>}
    {!loading && !error && current && <div className="discovery-layout"><article className="recommendation-card swipe-card" tabIndex={0} aria-label="Swipe candidate: left to pass, right to like" onKeyDown={event => { if (event.target !== event.currentTarget) return; if (event.key === 'ArrowLeft') { event.preventDefault(); void decide('PASS') }; if (event.key === 'ArrowRight' && visible) { event.preventDefault(); void decide('LIKE') } }} onPointerDown={event => { gesture.current = null; if (!(event.target as HTMLElement).closest('button, a, input, textarea')) gesture.current = { x: event.clientX, y: event.clientY } }} onPointerCancel={() => { gesture.current = null }} onPointerUp={event => { const start = gesture.current; gesture.current = null; if (start && Math.abs(event.clientX - start.x) > 90 && Math.abs(event.clientY - start.y) < 70 && (event.clientX < start.x || visible)) void decide(event.clientX > start.x ? 'LIKE' : 'PASS') }}><div className="card-top"><span className="avatar">{current.candidate.displayName.split(' ').slice(0, 2).map(name => name[0]).join('')}</span><div className="score"><strong>{current.compatibility}%</strong><span>compatibility score</span></div></div><div className="chips identity-badges"><span>{current.candidate.entityType === 'ORGANIZATION' ? 'Organization' : 'Individual'}</span><span>{current.candidate.matchingIntent.toLowerCase()}</span></div><h2>{current.candidate.displayName}</h2><p>{current.candidate.role}</p><p>{current.candidate.description}</p><p className="note">No GitHub connected</p><h3>Offers</h3><div className="chips">{current.candidate.skills.map(skill => <span key={skill}>{skill}</span>)}</div><h3>Needs</h3><p>{current.candidate.neededSkills.join(', ') || 'Open to complementary skills'}</p><p>{current.candidate.goal} · {current.candidate.weeklyHours} hours / week</p><p>{current.candidate.interests.join(' · ')}</p><div className="onboarding-controls"><button className="secondary" disabled={busy} onClick={() => void decide('PASS')}>Pass</button><button disabled={busy || !visible} onClick={() => void decide('LIKE')}>{busy ? 'Saving…' : 'Like collaborator ♥'}</button></div><p className="note">{index + 1} of {results.length}. Choices are saved. Matching requires both people to like each other.</p></article><section className="explanation"><span className="eyebrow">WHY YOU CONNECT</span><h2>Shared potential.</h2><ul>{current.evidence.map(text => <li key={text}>{text}</li>)}</ul><p className="note">Compatibility is a heuristic score, not a prediction of a successful partnership.</p></section></div>}
    {!loading && !error && !current && <section className="preview"><h2>{results.length ? 'You’ve explored this batch.' : 'Your network is just getting started.'}</h2><p>{results.length ? 'Refresh to discover again.' : 'No opted-in profiles currently meet your intent and compatibility preferences above 50%. Try again as more people join, or edit your preferences.'}</p><button onClick={() => setAttempt(value => value + 1)}>Refresh discovery</button></section>}
    {limited && <p className="note">This preview ranks up to 200 recently updated visible profiles. Broader retrieval is a future improvement.</p>}
  </section>
}
