import { useState } from 'react'
import { skills } from './profile'
type Suggestions = { offered: string[]; needed: string[]; unclassified: string[] }
export default function DescriptionSkills({ description, onAdd }: { description: string; onAdd: (kind: 'skills' | 'neededSkills', skill: string) => void }) {
 const [result, setResult] = useState<{ source: string; suggestions: Suggestions } | null>(null)
 const [busy, setBusy] = useState(false), [error, setError] = useState('')
 async function analyze() {
  if (busy || !description.trim()) return
  setBusy(true); setError('')
  const source = description
  try {
   const r = await fetch('/api/onboarding/skills', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ description: source }) })
   if (!r.ok) throw new Error('Skill suggestions are unavailable. You can still select skills manually in the next steps.')
   const suggestions = await r.json()
   if (['offered', 'needed', 'unclassified'].some(key => !Array.isArray(suggestions[key]) || suggestions[key].some((s: unknown) => typeof s !== 'string' || !skills.includes(s)))) throw new Error('Unexpected suggestions. Please select skills manually.')
   setResult({ source, suggestions })
  } catch (e) { setError(e instanceof Error ? e.message : 'Cannot analyze the description.') }
  finally { setBusy(false) }
 }
 const current = result?.source === description
 return <section className="preview"><button type="button" className="secondary" disabled={busy || !description.trim()} onClick={() => void analyze()}>{busy ? 'Reading your description…' : 'Suggest skills from description'}</button><p className="note">English skill suggestions look for phrases such as “I offer Java” and “I need Python”. Review them before adding; your selections change only when you choose Add.</p>{error && <p className="error" role="alert">{error}</p>}{result && !current && <p role="status">Your description changed. Suggest skills again to review current text.</p>}{result && current && <div aria-live="polite">{(['offered', 'needed', 'unclassified'] as const).map(kind => <div key={kind}><h3>{kind === 'offered' ? 'You offer' : kind === 'needed' ? 'You need' : 'Mentioned — choose the direction'}</h3>{!result.suggestions[kind].length && <p className="note">No clear suggestions.</p>}{result.suggestions[kind].map(skill => <div key={skill} className="actions"><span>{skill}</span>{kind !== 'needed' && <button type="button" className="secondary" onClick={() => onAdd('skills', skill)}>Add {skill} to offered skills</button>}{kind !== 'offered' && <button type="button" className="secondary" onClick={() => onAdd('neededSkills', skill)}>Add {skill} to needed skills</button>}</div>)}</div>)}</div>}</section>
}
