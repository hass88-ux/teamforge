import { useEffect, useState } from 'react'
import { accountRequest } from './accountApi'
import type { ProfileDraft } from './profile'

type PublicProfile = Omit<ProfileDraft, 'availability' | 'timezone'> & { id: string; accountType: 'REAL' }
type Result = { candidate: PublicProfile; compatibility: number; evidence: string[] }
export default function RealDiscover({ onBack }: { onBack: () => void }) {
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
  return <section className="discover"><div className="onboarding-top"><span className="demo-label">YOUR NETWORK</span><button className="text-button" onClick={onBack}>Your profile</button></div><h1>Find people<br/><em>ready to build.</em></h1>
    <section className="preview"><h2>Profile visibility</h2><p>{visible === null ? 'Loading your profile visibility…' : `Your profile is ${visible ? 'visible to signed-in members' : 'private'}.`} When enabled, discovery shares your name, description, type, intent, skills, interests, role preferences, goal, working style, and weekly commitment. Your email and selected schedule stay private.</p><button className="secondary" disabled={loading || busy || visible === null} onClick={() => void visibility()}>{busy ? 'Saving…' : visible ? 'Hide my profile from discovery' : 'Show my profile in discovery'}</button><p className="note">Visibility is optional. You can browse while private and change this setting anytime.</p></section>
    {error && <div className="error" role="alert"><p>{error}</p><button disabled={loading || busy} onClick={() => setAttempt(value => value + 1)}>Try again</button></div>}
    {loading && <p role="status">Finding compatible profiles…</p>}
    {!loading && !error && current && <div className="discovery-layout"><article className="recommendation-card"><div className="card-top"><span className="avatar">{current.candidate.displayName.split(' ').slice(0, 2).map(name => name[0]).join('')}</span><div className="score"><strong>{current.compatibility}%</strong><span>compatibility score</span></div></div><div className="chips identity-badges"><span>{current.candidate.entityType === 'ORGANIZATION' ? 'Organization' : 'Individual'}</span><span>{current.candidate.matchingIntent.toLowerCase()}</span></div><h2>{current.candidate.displayName}</h2><p>{current.candidate.role}</p><p>{current.candidate.description}</p><p className="note">No GitHub connected</p><h3>Offers</h3><div className="chips">{current.candidate.skills.map(skill => <span key={skill}>{skill}</span>)}</div><h3>Needs</h3><p>{current.candidate.neededSkills.join(', ') || 'Open to complementary skills'}</p><p>{current.candidate.goal} · {current.candidate.weeklyHours} hours / week</p><p>{current.candidate.interests.join(' · ')}</p><button className="secondary" onClick={() => setIndex(value => value + 1)}>Next profile →</button><p className="note">{index + 1} of {results.length}. Mutual likes and real messaging are coming next.</p></article><section className="explanation"><span className="eyebrow">WHY YOU CONNECT</span><h2>Shared potential.</h2><ul>{current.evidence.map(text => <li key={text}>{text}</li>)}</ul><p className="note">Compatibility is a heuristic score, not a prediction of a successful partnership.</p></section></div>}
    {!loading && !error && !current && <section className="preview"><h2>{results.length ? 'You’ve explored this batch.' : 'Your network is just getting started.'}</h2><p>{results.length ? 'Refresh to discover again.' : 'No opted-in profiles currently meet your intent and compatibility preferences above 50%. Try again as more people join, or edit your preferences.'}</p><button onClick={() => setAttempt(value => value + 1)}>Refresh discovery</button></section>}
    {limited && <p className="note">This preview ranks up to 200 recently updated visible profiles. Broader retrieval is a future improvement.</p>}
  </section>
}
