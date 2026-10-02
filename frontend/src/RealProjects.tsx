import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { accountRequest } from './accountApi'
import type { RealMatch } from './RealConversation'
import './Projects.css'

type Project = { id: string; owned: boolean; name: string; description: string; stage: string; yourStatus: string; members: { id: string; displayName: string; status: string }[] }
export default function RealProjects({ onBack }: { onBack: () => void }) {
 const [projects, setProjects] = useState<Project[]>([])
 const [matches, setMatches] = useState<RealMatch[]>([])
 const [name, setName] = useState(''), [description, setDescription] = useState(''), [stage, setStage] = useState('Idea')
 const [attempt, setAttempt] = useState(0), [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [error, setError] = useState('')
 const retry = useRef<{ payload: string; id: string } | null>(null)
 useEffect(() => {
  let active = true
  async function load() {
   setLoading(true)
   try {
    const [saved, network] = await Promise.all([accountRequest('/api/projects'), accountRequest('/api/matches')])
    if (!saved.ok || !network.ok) throw new Error('Cannot load your projects or matches. Log in again if your session expired, then retry.')
    const p = await saved.json(), m = await network.json()
    if (active) { setProjects(p); setMatches(m); setError('') }
   } catch (e) { if (active) setError(e instanceof Error ? e.message : 'Cannot load projects.') }
   finally { if (active) setLoading(false) }
  }
  void load(); return () => { active = false }
 }, [attempt])
 async function mutate(path: string, body: unknown) {
  const response = await accountRequest(path, 'POST', body)
  if (!response.ok) throw new Error(response.status === 409 ? 'This invitation or draft has already changed. Refresh projects to see its current status.' : response.status === 429 ? 'Preview limit reached: 20 owned projects or 10 invitations per project.' : response.status === 404 ? 'This project or match is no longer available.' : 'Cannot save this change. Check your details and try again.')
  return await response.json() as Project
 }
 async function create(event: FormEvent) {
  event.preventDefault(); if (busy || loading) return
  setBusy(true); setError('')
  const draft = { name: name.trim(), description: description.trim(), stage }
  const payload = JSON.stringify(draft)
  if (retry.current?.payload !== payload) retry.current = { payload, id: crypto.randomUUID() }
  try {
   const result = await mutate('/api/projects', { ...draft, clientId: retry.current.id })
   setProjects(current => [result, ...current.filter(p => p.id !== result.id)]); retry.current = null; setName(''); setDescription('')
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot save project.') }
  finally { setBusy(false) }
 }
 async function act(project: Project, action: 'INVITE' | 'ACCEPTED' | 'DECLINED', matchId?: string) {
  if (busy || loading) return
  setBusy(true); setError('')
  try {
   const result = await mutate(`/api/projects/${project.id}/${action === 'INVITE' ? 'members' : 'response'}`, action === 'INVITE' ? { matchId } : { status: action })
   setProjects(current => action === 'DECLINED' ? current.filter(p => p.id !== project.id) : current.map(p => p.id === result.id ? result : p))
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot update team.') }
  finally { setBusy(false) }
 }
 return <section className="projects"><div className="onboarding-top"><span className="demo-label">YOUR PROJECTS & TEAMS</span><button className="text-button" onClick={onBack}>Back to your profile</button></div><h1>Turn your idea<br/><em>into a team.</em></h1><p>Your projects are saved to your account. Invite existing matches; teammates join only after accepting.</p>
  {error && <p className="error" role="alert">{error}</p>}
  <button className="secondary" disabled={busy || loading} onClick={() => setAttempt(n => n + 1)}>Refresh projects</button>
  {loading && <p role="status">Loading projects…</p>}
  <form className="project-form" onSubmit={create}><h2>Create a saved project</h2><label className="input-label" htmlFor="saved-project-name">Project name</label><input id="saved-project-name" required maxLength={80} value={name} onChange={e => setName(e.target.value)}/><label className="input-label" htmlFor="saved-project-description">What are you building?</label><textarea id="saved-project-description" required maxLength={1000} value={description} onChange={e => setDescription(e.target.value)}/><label className="input-label" htmlFor="saved-project-stage">Stage</label><select id="saved-project-stage" value={stage} onChange={e => setStage(e.target.value)}>{['Idea', 'Prototype', 'In progress'].map(s => <option key={s}>{s}</option>)}</select><button disabled={busy || loading || !name.trim() || !description.trim()}>{busy ? 'Saving…' : 'Save project'}</button><p className="note">Projects are shared only with their owner and invited teammates. No email invitations are sent.</p></form>
  <section className="liked-list"><h2>Your saved projects and invitations</h2>{!loading && !projects.length && <p>No projects yet. Save an idea to start your team.</p>}<div className="summary-grid">{projects.map(p => <article key={p.id}><span className="demo-label">{p.owned ? 'PROJECT OWNER' : p.yourStatus === 'INVITED' ? 'TEAM INVITATION' : 'TEAM MEMBER'}</span><h3>{p.name}</h3><p>{p.description}</p><p>{p.stage}</p><h4>Team roster</h4><ul>{p.members.map(m => <li key={m.id}>{m.displayName} · {m.status.toLowerCase()}</li>)}</ul>{p.yourStatus === 'INVITED' && <div className="actions"><button disabled={busy || loading} onClick={() => void act(p, 'ACCEPTED')}>Join team</button><button className="secondary" disabled={busy || loading} onClick={() => void act(p, 'DECLINED')}>Decline invitation</button></div>}{p.owned && <><h4>Invite a match</h4>{!matches.length && <p>Mutual matches appear here when you connect with people in discovery.</p>}<div className="match-list">{matches.map(m => <button className="secondary" key={m.id} disabled={busy || loading} onClick={() => void act(p, 'INVITE', m.id)}>Invite {m.displayName}</button>)}</div><p className="note">Repeated invitations are safe. Declined invitations cannot be sent again in this preview.</p></>}</article>)}</div></section>
 </section>
}
