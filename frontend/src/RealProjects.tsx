import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { accountRequest } from './accountApi'
import type { RealMatch } from './RealConversation'
import './Projects.css'
import ProjectTasks from './ProjectTasks'

type Project = { id: string; owned: boolean; name: string; description: string; stage: string; revision: number; yourStatus: string; members: { id: string; displayName: string; status: string }[] }
function ProjectEditor({ project, onSaved, onCancel }: { project: Project; onSaved: (p: Project) => void; onCancel: () => void }) {
 const [name, setName] = useState(project.name), [description, setDescription] = useState(project.description), [stage, setStage] = useState(project.stage)
 const [busy, setBusy] = useState(false), [error, setError] = useState('')
 async function save(event: FormEvent) {
  event.preventDefault(); if (busy) return
  setBusy(true); setError('')
  try {
   const r = await accountRequest(`/api/projects/${project.id}`, 'PUT', { name: name.trim(), description: description.trim(), stage, revision: project.revision })
   if (!r.ok) throw new Error(r.status === 409 ? 'The project changed in another session. Cancel, refresh projects, and reopen the editor.' : 'Cannot save your edits. Please try again.')
   onSaved(await r.json())
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot save edits.') }
  finally { setBusy(false) }
 }
 return <form onSubmit={save}><label className="input-label" htmlFor="edit-project-name">Edit project name</label><input id="edit-project-name" required maxLength={80} value={name} onChange={e => setName(e.target.value)}/><label className="input-label" htmlFor="edit-project-description">Edit description</label><textarea id="edit-project-description" required maxLength={1000} value={description} onChange={e => setDescription(e.target.value)}/><label className="input-label" htmlFor="edit-project-stage">Edit stage</label><select id="edit-project-stage" value={stage} onChange={e => setStage(e.target.value)}>{['Idea', 'Prototype', 'In progress'].map(s => <option key={s}>{s}</option>)}</select>{error && <p className="error" role="alert">{error}</p>}<div className="actions"><button disabled={busy || !name.trim() || !description.trim()}>Save changes</button><button type="button" className="secondary" disabled={busy} onClick={onCancel}>Cancel editing</button></div></form>
}
export default function RealProjects({ onBack }: { onBack: () => void }) {
 const [projects, setProjects] = useState<Project[]>([])
 const [matches, setMatches] = useState<RealMatch[]>([])
 const [editing, setEditing] = useState<string | null>(null)
 const [confirmation, setConfirmation] = useState<{ project: string; member?: string } | null>(null)
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
 async function endMembership(project: Project, member?: string) {
  if (busy || loading) return
  setBusy(true); setError('')
  try {
   const r = await accountRequest(`/api/projects/${project.id}/${member ? `members/${member}/remove` : 'leave'}`, 'POST')
   if (!r.ok) throw new Error('Cannot update membership. Refresh projects and try again.')
   if (member) { const updated = await r.json(); setProjects(current => current.map(p => p.id === updated.id ? updated : p)) }
   else setProjects(current => current.filter(p => p.id !== project.id))
   setConfirmation(null)
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot update membership.') }
  finally { setBusy(false) }
 }
 return <section className="projects"><div className="onboarding-top"><span className="demo-label">YOUR PROJECTS & TEAMS</span><button className="text-button" onClick={onBack}>Back to your profile</button></div><h1>Turn your idea<br/><em>into a team.</em></h1><p>Your projects are saved to your account. Invite existing matches; teammates join only after accepting.</p>
  {error && <p className="error" role="alert">{error}</p>}
  <button className="secondary" disabled={busy || loading || editing !== null} onClick={() => setAttempt(n => n + 1)}>Refresh projects</button>
  {loading && <p role="status">Loading projects…</p>}
  <form className="project-form" onSubmit={create}><h2>Create a saved project</h2><label className="input-label" htmlFor="saved-project-name">Project name</label><input id="saved-project-name" required maxLength={80} value={name} onChange={e => setName(e.target.value)}/><label className="input-label" htmlFor="saved-project-description">What are you building?</label><textarea id="saved-project-description" required maxLength={1000} value={description} onChange={e => setDescription(e.target.value)}/><label className="input-label" htmlFor="saved-project-stage">Stage</label><select id="saved-project-stage" value={stage} onChange={e => setStage(e.target.value)}>{['Idea', 'Prototype', 'In progress'].map(s => <option key={s}>{s}</option>)}</select><button disabled={busy || loading || !name.trim() || !description.trim()}>{busy ? 'Saving…' : 'Save project'}</button><p className="note">Projects are shared only with their owner and invited teammates. No email invitations are sent.</p></form>
  <section className="liked-list"><h2>Your saved projects and invitations</h2>{!loading && !projects.length && <p>No projects yet. Save an idea to start your team.</p>}<div className="summary-grid">{projects.map(p => <article key={p.id}><span className="demo-label">{p.owned ? 'PROJECT OWNER' : p.yourStatus === 'INVITED' ? 'TEAM INVITATION' : 'TEAM MEMBER'}</span><h3>{p.name}</h3><p>{p.description}</p><p>{p.stage}</p>{p.owned && (editing === p.id ? <ProjectEditor key={p.id} project={p} onCancel={() => setEditing(null)} onSaved={updated => { setProjects(current => current.map(item => item.id === updated.id ? updated : item)); setEditing(null) }} /> : <button className="secondary" disabled={busy || loading || editing !== null} onClick={() => setEditing(p.id)}>Edit project</button>)}<ProjectTasks projectId={p.id} canEdit={p.owned || p.yourStatus === 'ACCEPTED'}/><h4>Team roster</h4><ul>{p.members.map(m => <li key={m.id}>{m.displayName} · {m.status.toLowerCase()}{p.owned && ['INVITED', 'ACCEPTED'].includes(m.status) && <button className="text-button" disabled={busy || loading || editing !== null} onClick={() => setConfirmation({ project: p.id, member: m.id })}>Remove {m.displayName}</button>}</li>)}</ul>{p.yourStatus === 'ACCEPTED' && <button className="secondary" disabled={busy || loading} onClick={() => setConfirmation({ project: p.id })}>Leave team</button>}{confirmation?.project === p.id && <div className="demo-disclosure"><p>{confirmation.member ? 'Remove this teammate or cancel their invitation?' : 'Leave this team?'} Access to this project will end. You cannot rejoin this project in the current preview.</p><button disabled={busy || loading} onClick={() => void endMembership(p, confirmation.member)}>Confirm {confirmation.member ? 'removal' : 'leave'}</button><button className="secondary" disabled={busy} onClick={() => setConfirmation(null)}>Keep membership</button></div>}{p.yourStatus === 'INVITED' && <div className="actions"><button disabled={busy || loading} onClick={() => void act(p, 'ACCEPTED')}>Join team</button><button className="secondary" disabled={busy || loading} onClick={() => void act(p, 'DECLINED')}>Decline invitation</button></div>}{p.owned && <><h4>Invite a match</h4>{!matches.length && <p>Mutual matches appear here when you connect with people in discovery.</p>}<div className="match-list">{matches.map(m => <button className="secondary" key={m.id} disabled={busy || loading} onClick={() => void act(p, 'INVITE', m.id)}>Invite {m.displayName}</button>)}</div><p className="note">Repeated invitations are safe. Declined invitations cannot be sent again in this preview.</p></>}</article>)}</div></section>
 </section>
}
