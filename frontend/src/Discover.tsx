import { useEffect, useReducer, useRef, useState } from 'react'
import type { ProfileDraft } from './profile'

import { demoReducer, initialDemoState, demoReciprocates } from './demoState'
import type { Recommendation } from './demoState'
import DemoConversation from './DemoConversation'

export default function Discover({ profile, onBack }: { profile: ProfileDraft; onBack: () => void }) {
  const gestureStart = useRef<{ x: number; y: number } | null>(null)
  const [recommendations, setRecommendations] = useState<Recommendation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const [index, setIndex] = useState(0)
  const [details, setDetails] = useState(false)
  const [session, dispatch] = useReducer(demoReducer, initialDemoState)
  const [activeMatch, setActiveMatch] = useState<string | null>(null)
  const [newMatch, setNewMatch] = useState<string | null>(null)
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
    if (!recommendations[index]) return
    const candidate = recommendations[index].candidate
    dispatch({ type: 'decide', recommendation: recommendations[index], liked })
    if (liked && demoReciprocates(recommendations[index])) setNewMatch(candidate.id)
    setAnnouncement(`${liked ? 'Liked' : 'Passed'} ${candidate.displayName}.`)
    setIndex(current => current + 1); setDetails(false)
  }
  const current = recommendations[index]
  const likes = recommendations.filter(item => session.decisions[item.candidate.id] === 'like').map(item => item.candidate)
  const openedMatch = session.matches.find(match => match.candidate.id === activeMatch)
  const announcedMatch = session.matches.find(match => match.candidate.id === newMatch)
  if (openedMatch) return <DemoConversation match={openedMatch} dispatch={dispatch} onBack={() => setActiveMatch(null)} />
  return <section className="discover"><div className="onboarding-top"><span className="demo-label">DEMO DISCOVERY · FICTIONAL PROFILES</span><button className="text-button" onClick={onBack}>Your profile</button></div><h1>Find your<br/><em>next collaborator.</em></h1><p className="onboarding-subtitle">Swipe right to like, left to pass, or use the buttons. Only compatible intents above 50% are shown, drawn from 600 fictional profiles plus an optional tailored example.</p><p className="sr-only" aria-live="polite">{announcement}</p>
  {announcedMatch && <section className="match-banner" role="status"><span className="demo-label">A DEMO MATCH · SIMULATED MUTUAL INTEREST</span><h2>You and {announcedMatch.candidate.displayName} could build together.</h2><p>The fictional candidate simulates a like back when its mutual compatibility score is above 50%. This is a demo rule, not a real person's decision.</p><div className="actions"><button onClick={() => { setActiveMatch(announcedMatch.candidate.id); setNewMatch(null) }}>Start simulated conversation</button><button className="secondary" onClick={() => setNewMatch(null)}>Keep discovering</button></div></section>}
  {loading && <div className="loading" role="status">Finding people who complement your profile…</div>}
  {error && <div role="alert" className="error"><p>{error}</p><button onClick={() => { setLoading(true); setError(''); setAttempt(value => value + 1) }}>Try again</button></div>}
  {!loading && !error && current && <div className="discovery-layout"><article className="recommendation-card swipe-card" tabIndex={0} aria-label="Swipe candidate: left to pass, right to like" onKeyDown={event => { if (event.target !== event.currentTarget) return; if (event.key === 'ArrowLeft') { event.preventDefault(); decide(false) }; if (event.key === 'ArrowRight') { event.preventDefault(); decide(true) } }} onPointerDown={event => { gestureStart.current = null; if ((event.target as HTMLElement).closest('button, a, input, summary, select, textarea')) return; gestureStart.current = { x: event.clientX, y: event.clientY } }} onPointerCancel={() => { gestureStart.current = null }} onPointerUp={event => { const start = gestureStart.current; gestureStart.current = null; if (start && Math.abs(event.clientX - start.x) > 90 && Math.abs(event.clientY - start.y) < 70) decide(event.clientX > start.x) }}><div className="card-top"><span className="avatar">{current.candidate.displayName.split(' ').slice(0, 2).map(name => name[0]).join('')}</span><div className="score"><strong>{current.compatibility}%</strong><span>compatibility score</span></div></div><p className="tag">{current.candidate.tailoredForDemo ? 'TAILORED DEMO CANDIDATE' : 'FICTIONAL DEMO PROFILE'}</p><div className="chips identity-badges"><span>{current.candidate.entityType === 'ORGANIZATION' ? 'Organization' : 'Individual'}</span><span>{current.candidate.matchingIntent.toLowerCase()}</span></div><h2>{current.candidate.displayName}</h2><p>{current.candidate.description}</p><p className="note">No GitHub connected</p><p>{current.candidate.headline}</p><div className="chips">{current.candidate.skills.map((skill, i) => <span key={`${skill}-${i}`}>{skill}</span>)}</div><p>{current.candidate.weeklyHours} hours / week · {current.candidate.workingStyle}</p><p>{current.candidate.interests.join(' · ')}</p><button className="secondary" onClick={() => setDetails(value => !value)} aria-expanded={details}>{details ? 'Hide profile' : 'View profile & projects'}</button>{details && <section className="profile-details"><h3>Collaboration preferences</h3><p>{current.candidate.goal} · Seeking {current.candidate.rolesSought.join(', ')}</p><p>Needs: {current.candidate.neededSkills.join(', ') || 'Open to complementary skills'}</p><p>Timezone: {current.candidate.timezone}</p><h3>Synthetic project history</h3>{current.candidate.projects.map(project => <article key={project.name}><h4>{project.name}</h4><p>{project.description}</p><p>{project.technologies.join(' · ')}</p></article>)}</section>}<div className="onboarding-controls"><button className="secondary" onClick={() => decide(false)}>Pass</button><button onClick={() => decide(true)}>Like collaborator ♥</button></div><p className="note">{index + 1} of {recommendations.length} recommendations · Likes are temporary demo choices. Focus the card to use left / right arrow keys.</p></article><section className="explanation"><span className="eyebrow">WHY YOU CONNECT</span><h2>Compatibility with evidence.</h2><ul>{current.evidence.map(evidence => <li key={evidence}>{evidence}</li>)}</ul><details><summary>Inspect scoring</summary><p>You → them: {current.forwardScore} / 100<br/>Them → you: {current.reverseScore} / 100</p><p>The displayed score is the harmonic mean of both directions, not a probability of a successful collaboration.</p><h3>Your directional contributions</h3>{Object.entries(current.contributions).map(([feature, points]) => <div className="feature-row" key={feature}><span>{feature}</span><strong>+{points}</strong></div>)}<p className="note">Model: {current.modelVersion}. Weights are an initial heuristic, not a trained or validated predictive model.</p></details>{current.candidate.tailoredForDemo && <p className="demo-disclosure">This fictional candidate was constructed from your demo preferences and then ranked with the same scoring function. Real-user matching will not guarantee a match.</p>}</section></div>}
  {!loading && !error && !current && <div className="preview"><h2>You've explored this demo batch.</h2><p>{likes.length} collaborator{likes.length === 1 ? '' : 's'} liked. Explore your simulated matches below to try messaging and coffee-chat proposals.</p><button onClick={() => { setIndex(0); dispatch({ type: 'reset' }); setNewMatch(null); setAnnouncement('Demo choices reset.') }}>Reset discovery</button></div>}
  {likes.length > 0 && <section className="liked-list"><h2>People you liked · demo only</h2><div className="chips">{likes.map(candidate => <span key={candidate.id}>{candidate.displayName}</span>)}</div><p className="note">No real person receives these demo actions. Simulated matches use the disclosed compatibility rule.</p></section>}
  {session.matches.length > 0 && <section className="liked-list"><h2>Your simulated matches</h2><div className="match-list">{session.matches.map(match => <button className="secondary" key={match.candidate.id} onClick={() => setActiveMatch(match.candidate.id)}><strong>{match.candidate.displayName}</strong><span>{match.compatibility}% · {match.messages.filter(message => message.sender === 'you').length} demo messages →</span></button>)}</div></section>}
  </section>
}
