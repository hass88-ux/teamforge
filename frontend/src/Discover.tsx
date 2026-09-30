import { useEffect, useState } from 'react'
import type { ProfileDraft } from './profile'

type Candidate = ProfileDraft & { id: string; accountType: 'DEMO'; headline: string; tailoredForDemo?: boolean; projects: { name: string; description: string; technologies: string[]; synthetic: boolean }[] }
type Recommendation = { candidate: Candidate; compatibility: number; forwardScore: number; reverseScore: number; contributions: Record<string, number>; evidence: string[]; overlapHours: number; modelVersion: string }

export default function Discover({ profile, onBack }: { profile: ProfileDraft; onBack: () => void }) {
  const [recommendations, setRecommendations] = useState<Recommendation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [index, setIndex] = useState(0)
  const [details, setDetails] = useState(false)
  const [likes, setLikes] = useState<Candidate[]>([])
  const [announcement, setAnnouncement] = useState('')
  useEffect(() => {
    const abort = new AbortController()
    fetch('/api/demo/recommendations', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(profile), signal: abort.signal })
      .then(async response => { if (!response.ok) throw new Error('Recommendations are temporarily unavailable. Please try again.'); return response.json() })
      .then(result => { if (result.accountType !== 'DEMO' || !Array.isArray(result.recommendations)) throw new Error('Unexpected recommendation response.'); setRecommendations(result.recommendations) })
      .catch(failure => { if (!abort.signal.aborted) setError(failure instanceof Error ? failure.message : 'Cannot reach recommendations.') })
      .finally(() => { if (!abort.signal.aborted) setLoading(false) })
    return () => abort.abort()
  }, [profile, attempt])
  function decide(liked: boolean) {
    const candidate = recommendations[index].candidate
    if (liked) setLikes(current => [...current, candidate])
    setAnnouncement(`${liked ? 'Liked' : 'Passed'} ${candidate.displayName}.`)
    setIndex(current => current + 1); setDetails(false)
  }
  const current = recommendations[index]
  return <section className="discover"><div className="onboarding-top"><span className="demo-label">DEMO DISCOVERY · FICTIONAL PROFILES</span><button className="text-button" onClick={onBack}>Your profile</button></div><h1>Find your<br/><em>next collaborator.</em></h1><p className="onboarding-subtitle">Ranked for both sides of the collaboration. Every profile here is synthetic.</p><p className="sr-only" aria-live="polite">{announcement}</p>
  {loading && <div className="loading" role="status">Finding people who complement your profile…</div>}
  {error && <div role="alert" className="error"><p>{error}</p><button onClick={() => { setLoading(true); setError(''); setAttempt(value => value + 1) }}>Try again</button></div>}
  {!loading && !error && current && <div className="discovery-layout"><article className="recommendation-card"><div className="card-top"><span className="avatar">{current.candidate.displayName.split(' ').map(name => name[0]).join('')}</span><div className="score"><strong>{current.compatibility}%</strong><span>compatibility score</span></div></div><p className="tag">{current.candidate.tailoredForDemo ? 'TAILORED DEMO CANDIDATE' : 'FICTIONAL DEMO PROFILE'}</p><h2>{current.candidate.displayName}</h2><p>{current.candidate.headline}</p><div className="chips">{current.candidate.skills.map((skill, i) => <span key={`${skill}-${i}`}>{skill}</span>)}</div><p>{current.candidate.weeklyHours} hours / week · {current.candidate.workingStyle}</p><p>{current.candidate.interests.join(' · ')}</p><button className="secondary" onClick={() => setDetails(value => !value)} aria-expanded={details}>{details ? 'Hide profile' : 'View profile & projects'}</button>{details && <section className="profile-details"><h3>Collaboration preferences</h3><p>{current.candidate.goal} · Seeking {current.candidate.rolesSought.join(', ')}</p><p>Timezone: {current.candidate.timezone}</p><h3>Synthetic project history</h3>{current.candidate.projects.map(project => <article key={project.name}><h4>{project.name}</h4><p>{project.description}</p><p>{project.technologies.join(' · ')}</p></article>)}</section>}<div className="onboarding-controls"><button className="secondary" onClick={() => decide(false)}>Pass</button><button onClick={() => decide(true)}>Like collaborator ♥</button></div><p className="note">{index + 1} of {recommendations.length} recommendations · Likes are temporary demo choices.</p></article><section className="explanation"><span className="eyebrow">WHY YOU CONNECT</span><h2>Compatibility with evidence.</h2><ul>{current.evidence.map(evidence => <li key={evidence}>{evidence}</li>)}</ul><details><summary>Inspect scoring</summary><p>You → them: {current.forwardScore} / 100<br/>Them → you: {current.reverseScore} / 100</p><p>The displayed score is the harmonic mean of both directions, not a probability of a successful collaboration.</p><h3>Your directional contributions</h3>{Object.entries(current.contributions).map(([feature, points]) => <div className="feature-row" key={feature}><span>{feature}</span><strong>+{points}</strong></div>)}<p className="note">Model: {current.modelVersion}. Weights are an initial heuristic, not a trained or validated predictive model.</p></details>{current.candidate.tailoredForDemo && <p className="demo-disclosure">This fictional candidate was constructed from your demo preferences and then ranked with the same scoring function. Real-user matching will not guarantee a match.</p>}</section></div>}
  {!loading && !error && !current && <div className="preview"><h2>You've explored this demo batch.</h2><p>{likes.length} collaborator{likes.length === 1 ? '' : 's'} liked. Mutual matches and messaging are the next features in development.</p><button onClick={() => { setIndex(0); setLikes([]); setAnnouncement('Demo choices reset.') }}>Reset discovery</button></div>}
  {likes.length > 0 && <section className="liked-list"><h2>People you liked · demo only</h2><div className="chips">{likes.map(candidate => <span key={candidate.id}>{candidate.displayName}</span>)}</div><p className="note">A like is not a mutual match. No real person receives these demo actions.</p></section>}
  </section>
}
