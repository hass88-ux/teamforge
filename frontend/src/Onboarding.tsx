import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { domains, emptyProfile, goals, roles, skills, stepIsComplete, styles } from './profile'
import type { ProfileDraft } from './profile'

const steps = ['Your starting point', 'What you bring', 'What sparks your interest', 'Your missing piece', 'Your working rhythm', 'Make time to build']
const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const hours = [8, 12, 18, 19, 20, 21]

function Choices({ label, options, selected, onChange, multiple = false }: { label: string; options: string[]; selected: string[]; onChange: (value: string[]) => void; multiple?: boolean }) {
  return <fieldset className="choice-group"><legend>{label}</legend><div className="choice-grid">{options.map(option => <button key={option} type="button" className={selected.includes(option) ? 'choice selected' : 'choice'} aria-pressed={selected.includes(option)} onClick={() => onChange(multiple ? selected.includes(option) ? selected.filter(item => item !== option) : [...selected, option] : [option])}>{option}{selected.includes(option) && <span aria-hidden="true"> ✓</span>}</button>)}</div></fieldset>
}

export default function Onboarding({ onExit }: { onExit: () => void }) {
  const [profile, setProfile] = useState<ProfileDraft>(emptyProfile)
  const [step, setStep] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [validated, setValidated] = useState(false)
  const heading = useRef<HTMLHeadingElement>(null)
  function update<K extends keyof ProfileDraft>(key: K, value: ProfileDraft[K]) { setProfile(current => ({ ...current, [key]: value })); setError('') }
  function move(next: number) { setStep(next); setError(''); requestAnimationFrame(() => heading.current?.focus()) }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!stepIsComplete(step, profile) || busy) return
    if (step < steps.length - 1) { move(step + 1); return }
    setBusy(true); setError('')
    try {
      const response = await fetch('/api/onboarding/validate', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...profile, displayName: profile.displayName.trim(), timezone: profile.timezone.trim() }) })
      if (!response.ok) throw new Error(response.status === 400 ? 'Please check your timezone and selections, then try again.' : 'The demo service is unavailable. Your answers are still here; please try again.')
      const result = await response.json()
      if (result.accountType !== 'DEMO' || result.persisted !== false) throw new Error('Unexpected response from the demo service. Please try again.')
      setValidated(true)
    } catch (failure) { setError(failure instanceof Error && failure.message !== 'Failed to fetch' ? failure.message : 'Cannot reach the demo service. Your answers are still here; please try again.') }
    finally { setBusy(false) }
  }
  if (validated) return <section className="onboarding complete"><span className="demo-label">DEMO PROFILE · NOT SAVED</span><h1>You're ready to build,<br/><em>{profile.displayName}.</em></h1><p>Your profile has been validated. Recommendations are the next feature in development.</p><div className="summary-grid"><article><h2>You bring</h2><p>{profile.role}</p><div className="chips">{profile.skills.map(skill => <span key={skill}>{skill}</span>)}</div></article><article><h2>You're looking for</h2><p>{profile.rolesSought.join(', ')}</p><p>{profile.goal} · {profile.weeklyHours} hours / week</p></article><article><h2>Your rhythm</h2><p>{profile.workingStyle}</p><p>{profile.availability.length} selected hours · {profile.timezone}</p></article></div><div className="actions"><button onClick={() => { setValidated(false); move(0) }}>Edit answers</button><button className="secondary" onClick={onExit}>Return home</button></div><p className="note">This draft lives only in this page. Resetting or reloading clears it.</p></section>
  return <section className="onboarding"><div className="onboarding-top"><span className="demo-label">TRY DEMO · NO ACCOUNT NEEDED</span><button className="text-button" onClick={onExit}>Exit demo</button></div><div className="progress-caption"><span>Step {step + 1} of {steps.length}</span><span>{Math.round(step / steps.length * 100)}% complete</span></div><progress value={step} max={steps.length} aria-label="Onboarding progress"/><form onSubmit={submit}><h1 ref={heading} tabIndex={-1}>{steps[step]}</h1><p className="onboarding-subtitle">{['Tell us a little about you. A first name or nickname is enough.', 'Pick the skills you enjoy putting into practice.', 'Choose the areas and kind of project you want to build.', 'Great collaborators bring something different to the table.', 'Be realistic. A compatible rhythm helps projects last.', 'Select hours you are usually free, in your local timezone.'][step]}</p>
    {step === 0 && <><label className="input-label" htmlFor="display-name">What should we call you?</label><input id="display-name" autoComplete="off" maxLength={60} value={profile.displayName} onChange={event => update('displayName', event.target.value)} placeholder="Your name or nickname"/><Choices label="What best describes you?" options={roles} selected={[profile.role]} onChange={value => update('role', value[0])}/></>}
    {step === 1 && <Choices label="Your strongest skills — choose a few" options={skills} selected={profile.skills} onChange={value => update('skills', value)} multiple/>}
    {step === 2 && <><Choices label="What do you want to build?" options={domains} selected={profile.interests} onChange={value => update('interests', value)} multiple/><Choices label="What are you looking for?" options={goals} selected={[profile.goal]} onChange={value => update('goal', value[0])}/></>}
    {step === 3 && <Choices label="Which roles would complement you?" options={roles.filter(role => role !== 'Student')} selected={profile.rolesSought} onChange={value => update('rolesSought', value.slice(0, 10))} multiple/>}
    {step === 4 && <><label className="input-label" htmlFor="hours">How much time can you realistically commit?</label><div className="range-value">{profile.weeklyHours} hours / week</div><input id="hours" type="range" min={1} max={30} value={profile.weeklyHours} onChange={event => update('weeklyHours', Number(event.target.value))}/><Choices label="Which working style feels most like you?" options={styles} selected={[profile.workingStyle]} onChange={value => update('workingStyle', value[0])}/></>}
    {step === 5 && <><label className="input-label" htmlFor="timezone">Your timezone</label><input id="timezone" maxLength={80} value={profile.timezone} onChange={event => update('timezone', event.target.value)} placeholder="America/New_York"/><p className="note">Each selection represents one hour. You can change this later.</p><div className="availability-scroll"><table className="availability"><caption>Weekly availability in {profile.timezone || 'your timezone'}</caption><thead><tr><th scope="col">Time</th>{days.map(day => <th key={day} scope="col">{day}</th>)}</tr></thead><tbody>{hours.map(hour => <tr key={hour}><th scope="row">{String(hour).padStart(2, '0')}:00</th>{days.map((day, index) => { const slot = index * 24 + hour; const selected = profile.availability.includes(slot); return <td key={day}><button type="button" className={selected ? 'time-slot selected' : 'time-slot'} aria-label={`${day} ${hour}:00`} aria-pressed={selected} onClick={() => update('availability', selected ? profile.availability.filter(value => value !== slot) : [...profile.availability, slot])}>{selected ? '✓' : '+'}</button></td> })}</tr>)}</tbody></table></div></>}
    {error && <p role="alert" className="error">{error}</p>}<div className="onboarding-controls"><button type="button" className="secondary" onClick={() => move(step - 1)} disabled={step === 0 || busy}>Back</button><button type="submit" disabled={!stepIsComplete(step, profile) || busy}>{busy ? 'Checking your profile…' : step === steps.length - 1 ? 'Finish demo profile ↗' : 'Continue →'}</button></div><p className="note">Demo answers are temporary. No email, GitHub account, or private information required.</p></form></section>
}
