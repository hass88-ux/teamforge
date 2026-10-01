import type { ProfileDraft } from './profile'
export type Candidate = ProfileDraft & { id: string; accountType: 'DEMO'; headline: string; tailoredForDemo?: boolean; projects: { name: string; description: string; technologies: string[]; synthetic: boolean }[] }
export type Recommendation = { candidate: Candidate; compatibility: number; forwardScore: number; reverseScore: number; contributions: Record<string, number>; evidence: string[]; overlapHours: number; modelVersion: string }
export type DemoMessage = { id: string; sender: 'you' | 'simulation'; text: string; sentAt: string }
export type CoffeeProposal = { id: string; kind: 'Virtual coffee' | 'Intro call' | 'Project discussion'; startsAt: string; timezone: string; note: string; status: 'proposed' | 'accepted' | 'declined' }
export type DemoMatch = { candidate: Candidate; compatibility: number; messages: DemoMessage[]; proposals: CoffeeProposal[] }
export type DemoState = { decisions: Record<string, 'like' | 'pass'>; matches: DemoMatch[] }
export const initialDemoState: DemoState = { decisions: {}, matches: [] }
export type DemoAction =
  | { type: 'decide'; recommendation: Recommendation; liked: boolean }
  | { type: 'message'; candidateId: string; text: string; sentAt: string; id: string }
  | { type: 'propose'; candidateId: string; proposal: CoffeeProposal; now: string }
  | { type: 'respond'; candidateId: string; proposalId: string; status: 'accepted' | 'declined' }
  | { type: 'reset' }

// Demo consent is a disclosed simulation rule, never a real person's decision.
export function demoReciprocates(recommendation: Recommendation): boolean {
  return recommendation.candidate.accountType === 'DEMO' && recommendation.reverseScore >= 70
}

export function demoReducer(state: DemoState, action: DemoAction): DemoState {
  if (action.type === 'reset') return initialDemoState
  if (action.type === 'decide') {
    const { candidate } = action.recommendation
    if (candidate.accountType !== 'DEMO' || state.decisions[candidate.id]) return state
    const match = action.liked && demoReciprocates(action.recommendation)
    return { decisions: { ...state.decisions, [candidate.id]: action.liked ? 'like' : 'pass' }, matches: match ? [...state.matches, { candidate, compatibility: action.recommendation.compatibility, messages: [], proposals: [] }] : state.matches }
  }
  const match = state.matches.find(item => item.candidate.id === action.candidateId)
  if (!match) return state
  let updated = match
  if (action.type === 'message') {
    const text = action.text.trim()
    if (!text || text.length > 1000 || !Number.isFinite(Date.parse(action.sentAt)) || match.messages.some(message => message.id === action.id)) return state
    updated = { ...match, messages: [...match.messages,
      { id: action.id, sender: 'you', text, sentAt: action.sentAt },
      { id: `${action.id}-reply`, sender: 'simulation', text: 'Simulated reply: Thanks for the introduction! What problem would you like to work on together? You can also try a coffee-chat proposal below.', sentAt: action.sentAt }] }
  }
  if (action.type === 'propose') {
    const proposal = action.proposal
    if (!Number.isFinite(Date.parse(proposal.startsAt)) || !Number.isFinite(Date.parse(action.now)) || Date.parse(proposal.startsAt) <= Date.parse(action.now) || proposal.note.length > 500 || proposal.status !== 'proposed' || match.proposals.some(item => item.id === proposal.id)) return state
    updated = { ...match, proposals: [...match.proposals, proposal] }
  }
  if (action.type === 'respond') {
    updated = { ...match, proposals: match.proposals.map(proposal => proposal.id === action.proposalId && proposal.status === 'proposed' ? { ...proposal, status: action.status } : proposal) }
  }
  return { ...state, matches: state.matches.map(item => item.candidate.id === action.candidateId ? updated : item) }
}
